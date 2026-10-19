package com.gokova.myanimelist.feature.details.data.repository

import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.util.UserAnimeListSyncCoordinator
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.di.IoDispatcher
import com.gokova.myanimelist.feature.details.data.mapper.AnimeDetailsMapper
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.domain.repository.AnimeDetailsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeDetailsRepositoryImpl
    @Inject
    constructor(
        private val malApiService: MalApiService,
        private val userAnimeListDao: UserAnimeListDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val syncCoordinator: UserAnimeListSyncCoordinator = UserAnimeListSyncCoordinator(),
    ) : AnimeDetailsRepository {
        private val freshDetailsCache = MutableStateFlow<Map<Long, AnimeDetails>>(emptyMap())
        private val freshEntityCache = MutableStateFlow<Map<Long, AnimeEntity>>(emptyMap())

        override fun observeAnimeDetails(animeId: Long): Flow<AnimeDetails?> =
            combine(
                userAnimeListDao.observeAnimeEntityById(animeId),
                userAnimeListDao.observeUserAnimeById(animeId),
                freshDetailsCache,
            ) { entity, userAnimeItem, cache ->
                val cachedFresh = cache[animeId]
                cachedFresh?.copy(
                    isInUserList = userAnimeItem != null,
                    userStatus = userAnimeItem?.userAnime?.status,
                    userScore = userAnimeItem?.userAnime?.score?.takeIf { it > 0 },
                    numWatchedEpisodes = userAnimeItem?.userAnime?.numEpisodesWatched ?: 0,
                    isRewatching = userAnimeItem?.userAnime?.isRewatching ?: false,
                ) ?: entity?.let {
                    AnimeDetailsMapper.toDomain(it, userAnimeItem?.userAnime)
                }
            }.flowOn(ioDispatcher)

        override suspend fun fetchFreshAnimeDetails(animeId: Long): Result<AnimeDetails> =
            syncCoordinator.withLock {
                withContext(ioDispatcher) {
                    try {
                        val dto = malApiService.getAnimeDetails(animeId)
                        val entity = AnimeDetailsMapper.toAnimeEntity(dto)
                        freshEntityCache.update { it.withBoundedEntry(animeId to entity) }

                        val existingEntity = userAnimeListDao.getAnimeEntityById(animeId)
                        val hasExistingTrackedEntity = existingEntity != null
                        val isInRemoteUserList = dto.myListStatus != null

                        if (hasExistingTrackedEntity || isInRemoteUserList) {
                            userAnimeListDao.upsertAnimes(listOf(entity))
                        }

                        val localBeforeUpsert =
                            userAnimeListDao.getUserAnimeById(animeId)?.userAnime
                        val isStaleRemote =
                            isRemoteStatusOlderThanLocal(
                                remoteUpdatedAt = dto.myListStatus?.updatedAt,
                                localUpdatedAt = localBeforeUpsert?.updatedAt,
                            )

                        val statusDto = dto.myListStatus
                        if (statusDto != null && !isStaleRemote) {
                            val userAnime =
                                UserAnimeListEntity(
                                    animeId = dto.id,
                                    status = statusDto.status ?: "plan_to_watch",
                                    score = statusDto.score ?: 0,
                                    numEpisodesWatched = statusDto.numEpisodesWatched ?: 0,
                                    updatedAt = statusDto.updatedAt ?: "",
                                    isRewatching = statusDto.isRewatching ?: false,
                                )
                            userAnimeListDao.upsertUserAnimeList(listOf(userAnime))
                        }

                        val effectiveUserAnime =
                            if (isStaleRemote) {
                                localBeforeUpsert
                            } else {
                                userAnimeListDao.getUserAnimeById(animeId)?.userAnime
                            }
                        val domainDetails = AnimeDetailsMapper.toDomain(dto, effectiveUserAnime)
                        freshDetailsCache.update { it.withBoundedEntry(animeId to domainDetails) }
                        Result.success(domainDetails)
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        AppLog.network.e(e) { "Failed to fetch details for animeId=$animeId" }
                        Result.failure(e)
                    }
                }
            }

        override suspend fun addAnimeToPlanToWatch(animeId: Long): Result<Unit> =
            syncCoordinator.withLock {
                withContext(ioDispatcher) {
                    try {
                        val statusDto =
                            malApiService.updateMyListStatus(
                                animeId = animeId,
                                status = "plan_to_watch",
                                numWatchedEpisodes = 0,
                                score = 0,
                            )

                        val existingEntity = userAnimeListDao.getAnimeEntityById(animeId)
                        if (existingEntity == null) {
                            freshEntityCache.value[animeId]?.let { cachedEntity ->
                                userAnimeListDao.upsertAnimes(listOf(cachedEntity))
                            }
                        }

                        val userAnime =
                            UserAnimeListEntity(
                                animeId = animeId,
                                status = statusDto.status ?: "plan_to_watch",
                                score = statusDto.score ?: 0,
                                numEpisodesWatched = statusDto.numEpisodesWatched ?: 0,
                                updatedAt = statusDto.updatedAt ?: "",
                                isRewatching = statusDto.isRewatching ?: false,
                            )
                        userAnimeListDao.addAnimeToUserListAndClean(userAnime)
                        freshDetailsCache.update { current ->
                            val existing = current[animeId]
                            if (existing != null) {
                                val updated =
                                    existing.copy(
                                        isInUserList = true,
                                        userStatus = "plan_to_watch",
                                    )
                                current.withBoundedEntry(animeId to updated)
                            } else {
                                current
                            }
                        }

                        AppLog.domain.i {
                            "Successfully added animeId=$animeId to plan_to_watch and pruned from recs/new seasons"
                        }
                        Result.success(Unit)
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        AppLog.network.e(e) { "Failed to add animeId=$animeId to plan_to_watch" }
                        Result.failure(e)
                    }
                }
            }

        private fun isRemoteStatusOlderThanLocal(
            remoteUpdatedAt: String?,
            localUpdatedAt: String?,
        ): Boolean {
            if (localUpdatedAt.isNullOrBlank() || remoteUpdatedAt.isNullOrBlank()) return false
            val localTime = parseIsoTimestamp(localUpdatedAt)
            val remoteTime = parseIsoTimestamp(remoteUpdatedAt)
            return localTime > 0L && remoteTime > 0L && remoteTime < localTime
        }

        private fun parseIsoTimestamp(isoString: String?): Long {
            if (isoString.isNullOrBlank()) return 0L
            return ISO_PATTERNS.firstNotNullOfOrNull { pattern ->
                try {
                    val sdf =
                        SimpleDateFormat(pattern, Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                    sdf.parse(isoString)?.time
                } catch (_: ParseException) {
                    null
                }
            } ?: 0L
        }

        private fun <K, V> Map<K, V>.withBoundedEntry(
            entry: Pair<K, V>,
            maxSize: Int = MAX_CACHE_SIZE,
        ): Map<K, V> {
            val updated = this - entry.first + entry
            return if (updated.size > maxSize) {
                updated.entries.drop(updated.size - maxSize).associate { it.key to it.value }
            } else {
                updated
            }
        }

        companion object {
            const val MAX_CACHE_SIZE = 10
            private val ISO_PATTERNS =
                arrayOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd",
                )
        }
    }
