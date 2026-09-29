package com.gokova.myanimelist.feature.details.data.repository

import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeDetailsRepositoryImpl
    @Inject
    constructor(
        private val malApiService: MalApiService,
        private val userAnimeListDao: UserAnimeListDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : AnimeDetailsRepository {
        private val freshDetailsCache = MutableStateFlow<Map<Long, AnimeDetails>>(emptyMap())

        override fun observeAnimeDetails(animeId: Long): Flow<AnimeDetails?> =
            combine(
                userAnimeListDao.observeAnimeEntityById(animeId),
                userAnimeListDao.observeUserAnimeById(animeId),
                freshDetailsCache,
            ) { entity, userAnimeItem, cache ->
                val cachedFresh = cache[animeId]
                cachedFresh?.copy(
                    isInUserList = userAnimeItem != null || cachedFresh.isInUserList,
                    userStatus = userAnimeItem?.userAnime?.status ?: cachedFresh.userStatus,
                    userScore =
                        userAnimeItem?.userAnime?.score?.takeIf { it > 0 } ?: cachedFresh.userScore,
                ) ?: entity?.let {
                    AnimeDetailsMapper.toDomain(it, userAnimeItem?.userAnime)
                }
            }.flowOn(ioDispatcher)

        override suspend fun fetchFreshAnimeDetails(animeId: Long): Result<AnimeDetails> =
            withContext(ioDispatcher) {
                try {
                    val dto = malApiService.getAnimeDetails(animeId)
                    val entity = AnimeDetailsMapper.toAnimeEntity(dto)
                    userAnimeListDao.upsertAnimes(listOf(entity))

                    dto.myListStatus?.let { statusDto ->
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

                    val localUserAnime = userAnimeListDao.getUserAnimeById(animeId)?.userAnime
                    val domainDetails = AnimeDetailsMapper.toDomain(dto, localUserAnime)
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

        override suspend fun addAnimeToPlanToWatch(animeId: Long): Result<Unit> =
            withContext(ioDispatcher) {
                try {
                    val statusDto =
                        malApiService.updateMyListStatus(
                            animeId = animeId,
                            status = "plan_to_watch",
                            numWatchedEpisodes = 0,
                            score = 0,
                        )

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

        private fun Map<Long, AnimeDetails>.withBoundedEntry(
            entry: Pair<Long, AnimeDetails>,
            maxSize: Int = MAX_CACHE_SIZE,
        ): Map<Long, AnimeDetails> {
            val updated = this - entry.first + entry
            return if (updated.size > maxSize) {
                updated.entries.drop(updated.size - maxSize).associate { it.key to it.value }
            } else {
                updated
            }
        }

        companion object {
            const val MAX_CACHE_SIZE = 10
        }
    }
