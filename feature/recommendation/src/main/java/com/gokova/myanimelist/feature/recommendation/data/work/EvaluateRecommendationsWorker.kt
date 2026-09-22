package com.gokova.myanimelist.feature.recommendation.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gokova.myanimelist.core.database.dao.RecommendationDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.feature.recommendation.data.mapper.RecommendationMapper
import com.gokova.myanimelist.feature.recommendation.domain.algorithm.RecommendationScorer
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationConstants.MIN_USER_LIST_THRESHOLD
import com.gokova.myanimelist.feature.recommendation.domain.model.UserTasteProfileItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class EvaluateRecommendationsWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val recommendationDao: RecommendationDao,
        private val userAnimeListDao: UserAnimeListDao,
    ) : CoroutineWorker(context, params) {
        private val scorer = RecommendationScorer()

        override suspend fun doWork(): Result {
            AppLog.domain.i { "EvaluateRecommendationsWorker started" }
            return try {
                val userAnimeList = userAnimeListDao.getAllUserAnime()
                if (userAnimeList.size < MIN_USER_LIST_THRESHOLD) {
                    AppLog.domain.i {
                        "User anime count (${userAnimeList.size}) < " +
                            "$MIN_USER_LIST_THRESHOLD, aborting"
                    }
                    recommendationDao.clearCandidates()
                } else {
                    val candidateEntities = recommendationDao.getCandidateAnimes()
                    if (candidateEntities.isNotEmpty()) {
                        processCandidates(userAnimeList, candidateEntities)
                    } else {
                        AppLog.domain.i { "No candidates found in database to evaluate" }
                    }
                }
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                AppLog.domain.e(e) { "EvaluateRecommendationsWorker encountered error" }
                Result.failure()
            }
        }

        private suspend fun processCandidates(
            userAnimeList: List<UserAnimeListItem>,
            candidateEntities: List<AnimeEntity>,
        ) {
            val userTasteItems =
                userAnimeList.map { item ->
                    UserTasteProfileItem(
                        animeId = item.anime.id,
                        genres = item.anime.genres?.map { it.name } ?: emptyList(),
                        userScore = item.userAnime.score.takeIf { it > 0 },
                    )
                }

            val candidateItems =
                candidateEntities.map {
                    RecommendationMapper.toCandidateAnimeItem(it)
                }

            val evaluation = scorer.evaluate(userTasteItems, candidateItems)

            val recommendationEntities =
                evaluation.recommendations.map {
                    RecommendationMapper.toRecommendationEntity(it)
                }

            recommendationDao.saveEvaluatedRecommendations(
                recommendations = recommendationEntities,
                discardedAnimeIds = evaluation.discardedAnimeIds,
            )

            AppLog.domain.i {
                "EvaluateRecommendationsWorker complete: " +
                    "${recommendationEntities.size} recommendations stored, " +
                    "${evaluation.discardedAnimeIds.size} discarded candidates pruned"
            }
        }
    }
