package com.gokova.myanimelist.feature.mylist.data.repository

import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.util.UserAnimeListSyncCoordinator
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.model.UserAnimeListUpdate
import com.gokova.myanimelist.core.domain.repository.UserAnimeListStatusRepository
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.di.IoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAnimeListStatusRepositoryImpl
    @Inject
    constructor(
        private val malApiService: MalApiService,
        private val userAnimeListDao: UserAnimeListDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val syncCoordinator: UserAnimeListSyncCoordinator = UserAnimeListSyncCoordinator(),
    ) : UserAnimeListStatusRepository {
        override suspend fun updateAnimeStatus(
            animeId: Long,
            params: UpdateUserAnimeStatusParams,
        ): Result<UserAnimeListUpdate> =
            syncCoordinator.withLock {
                withContext(ioDispatcher) {
                    try {
                        val statusDto =
                            malApiService.updateMyListStatus(
                                animeId = animeId,
                                status = params.status,
                                isRewatching = params.isRewatching,
                                score = params.score,
                                numWatchedEpisodes = params.numWatchedEpisodes,
                            )

                        ensureAnimeEntityExists(animeId)

                        val userAnime =
                            UserAnimeListEntity(
                                animeId = animeId,
                                status = statusDto.status ?: params.status ?: "",
                                score = statusDto.score ?: params.score ?: 0,
                                numEpisodesWatched =
                                    statusDto.numEpisodesWatched ?: params.numWatchedEpisodes ?: 0,
                                updatedAt = statusDto.updatedAt ?: "",
                                isRewatching =
                                    statusDto.isRewatching ?: params.isRewatching ?: false,
                            )

                        userAnimeListDao.addAnimeToUserListAndClean(userAnime)

                        val result =
                            UserAnimeListUpdate(
                                animeId = userAnime.animeId,
                                status = userAnime.status,
                                score = userAnime.score,
                                numEpisodesWatched = userAnime.numEpisodesWatched,
                                isRewatching = userAnime.isRewatching,
                                updatedAt = userAnime.updatedAt,
                            )
                        AppLog.domain.i { "Successfully updated list status for animeId=$animeId" }
                        Result.success(result)
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        AppLog.network.e(e) { "Failed to update list status for animeId=$animeId" }
                        Result.failure(e)
                    }
                }
            }

        override suspend fun deleteAnimeFromList(animeId: Long): Result<Unit> =
            syncCoordinator.withLock {
                withContext(ioDispatcher) {
                    try {
                        val response = malApiService.deleteMyListStatus(animeId)
                        if (response.isSuccessful || response.code() == HTTP_NOT_FOUND) {
                            userAnimeListDao.deleteUserAnime(animeId)
                            AppLog.domain.i {
                                "Successfully removed animeId=$animeId from user list"
                            }
                            Result.success(Unit)
                        } else {
                            val error = HttpException(response)
                            AppLog.network.e(
                                error,
                            ) { "Failed to delete animeId=$animeId: ${response.code()}" }
                            Result.failure(error)
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        AppLog.network.e(
                            e,
                        ) { "Exception while deleting animeId=$animeId from user list" }
                        Result.failure(e)
                    }
                }
            }

        private suspend fun ensureAnimeEntityExists(animeId: Long) {
            val existing = userAnimeListDao.getAnimeEntityById(animeId)
            if (existing == null) {
                try {
                    val details = malApiService.getAnimeDetails(animeId)
                    val entity =
                        AnimeEntity(
                            id = details.id,
                            title = details.title,
                            titleEnglish = details.alternativeTitles?.en,
                            mainPictureMedium = details.mainPicture?.medium,
                            mainPictureLarge = details.mainPicture?.large,
                            mediaType = details.mediaType,
                            airingStatus = details.status,
                            numEpisodes = details.numEpisodes,
                            startSeasonYear = details.startSeason?.year,
                            startSeasonSeason = details.startSeason?.season,
                            meanScore = details.mean,
                            synopsis = details.synopsis,
                        )
                    userAnimeListDao.upsertAnimes(listOf(entity))
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.network.w(e) { "Could not fetch entity for animeId=$animeId" }
                }
            }
        }

        companion object {
            private const val HTTP_NOT_FOUND = 404
        }
    }
