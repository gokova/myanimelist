package com.gokova.myanimelist.feature.recommendation.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gokova.myanimelist.core.database.dao.RecommendationDao
import com.gokova.myanimelist.core.database.entity.RecommendationCandidateEntity
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.util.isRetryableNetworkError
import com.gokova.myanimelist.feature.recommendation.data.mapper.RecommendationMapper
import com.gokova.myanimelist.feature.recommendation.data.remote.RecommendationRemoteDataSource
import com.gokova.myanimelist.feature.recommendation.data.remote.SeasonalPeriodCalculator
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationConstants.MIN_USER_LIST_THRESHOLD
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

@HiltWorker
class FetchCandidatesWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val remoteDataSource: RecommendationRemoteDataSource,
        private val recommendationDao: RecommendationDao,
        private val seasonalCalculator: SeasonalPeriodCalculator,
        private val newSeasonScheduler: NewSeasonScheduler,
    ) : CoroutineWorker(context, params) {
        private val isManual: Boolean
            get() = inputData.getBoolean(RecommendationScheduler.KEY_IS_MANUAL, false)

        override suspend fun doWork(): Result =
            syncMutex.withLock {
                AppLog.domain.i {
                    "FetchCandidatesWorker started (workId=$id, manual=$isManual, " +
                        "attempt=$runAttemptCount)"
                }
                try {
                    val userAnimeIds = recommendationDao.getUserAnimeIds().toSet()
                    if (userAnimeIds.size < MIN_USER_LIST_THRESHOLD) {
                        AppLog.domain.i {
                            "User anime list has ${userAnimeIds.size} < " +
                                "$MIN_USER_LIST_THRESHOLD, skipping"
                        }
                    } else {
                        if (newSeasonScheduler.hasActiveOneTimeWork()) {
                            AppLog.domain.i {
                                "FetchCandidatesWorker waiting for New Seasons work " +
                                    "(workId=$id)"
                            }
                            return@withLock Result.retry()
                        }
                        val excludedAnimeIds =
                            recommendationDao
                                .getExcludedCandidateAnimeIds()
                                .toSet()
                        fetchAndStoreCandidates(excludedAnimeIds)
                    }
                    Result.success()
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    if (isRetryableNetworkError(e)) {
                        val status = (e as? HttpException)?.code()?.toString() ?: "network"
                        AppLog.domain.w(e) {
                            "FetchCandidatesWorker retryable failure " +
                                "(workId=$id, status=$status)"
                        }
                        Result.retry()
                    } else {
                        AppLog.domain.e(e) {
                            "FetchCandidatesWorker encountered fatal error (workId=$id)"
                        }
                        Result.failure()
                    }
                }
            }

        private suspend fun fetchAndStoreCandidates(excludedAnimeIds: Set<Long>) {
            val topRanking = remoteDataSource.fetchTopRankingAnime()
            val targetSeasons = seasonalCalculator.getTargetSeasons()
            val seasonalAnime = mutableListOf<AnimeNodeDto>()

            for (season in targetSeasons) {
                val nodes =
                    remoteDataSource.fetchSeasonalAnime(
                        year = season.year,
                        season = season.season,
                    )
                seasonalAnime.addAll(nodes)
            }

            val allCandidates =
                (topRanking + seasonalAnime)
                    .distinctBy { it.id }
                    .filter { it.id !in excludedAnimeIds }

            val animeEntities = allCandidates.map { RecommendationMapper.toAnimeEntity(it) }
            val candidateEntities =
                allCandidates.map {
                    RecommendationCandidateEntity(animeId = it.id, source = "candidate")
                }

            recommendationDao.upsertAnimes(animeEntities)
            recommendationDao.upsertCandidates(candidateEntities)

            AppLog.domain.i {
                "FetchCandidatesWorker finished (workId=$id): " +
                    "${allCandidates.size} candidates saved"
            }
        }

        companion object {
            private val syncMutex = Mutex()
        }
    }
