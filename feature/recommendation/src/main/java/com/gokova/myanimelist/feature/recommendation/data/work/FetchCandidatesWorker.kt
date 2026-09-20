package com.gokova.myanimelist.feature.recommendation.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gokova.myanimelist.core.database.dao.RecommendationDao
import com.gokova.myanimelist.core.database.entity.RecommendationCandidateEntity
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.feature.recommendation.data.mapper.RecommendationMapper
import com.gokova.myanimelist.feature.recommendation.data.remote.RecommendationRemoteDataSource
import com.gokova.myanimelist.feature.recommendation.data.remote.SeasonalPeriodCalculator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.io.IOException

@HiltWorker
class FetchCandidatesWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val remoteDataSource: RecommendationRemoteDataSource,
        private val recommendationDao: RecommendationDao,
        private val seasonalCalculator: SeasonalPeriodCalculator,
        private val scheduler: RecommendationScheduler,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            AppLog.domain.i { "FetchCandidatesWorker started" }
            return try {
                val userAnimeIds = recommendationDao.getUserAnimeIds().toSet()
                if (userAnimeIds.size < MIN_USER_LIST_THRESHOLD) {
                    AppLog.domain.i {
                        "User anime list has ${userAnimeIds.size} < " +
                            "$MIN_USER_LIST_THRESHOLD, skipping"
                    }
                } else {
                    fetchAndStoreCandidates(userAnimeIds)
                }
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                AppLog.domain.w(e) { "FetchCandidatesWorker failed due to network error" }
                Result.retry()
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                AppLog.domain.e(e) { "FetchCandidatesWorker encountered fatal error" }
                Result.failure()
            }
        }

        private suspend fun fetchAndStoreCandidates(userAnimeIds: Set<Long>) {
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
                    .filter { it.id !in userAnimeIds }

            val animeEntities = allCandidates.map { RecommendationMapper.toAnimeEntity(it) }
            val candidateEntities =
                allCandidates.map {
                    RecommendationCandidateEntity(animeId = it.id, source = "candidate")
                }

            recommendationDao.upsertAnimes(animeEntities)
            recommendationDao.upsertCandidates(candidateEntities)

            val isManual =
                inputData.getBoolean(RecommendationScheduler.KEY_IS_MANUAL, false)
            scheduler.enqueueEvaluation(isManual)

            AppLog.domain.i {
                "FetchCandidatesWorker finished: ${allCandidates.size} candidates saved"
            }
        }

        companion object {
            private const val MIN_USER_LIST_THRESHOLD = 5
        }
    }
