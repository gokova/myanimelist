package com.gokova.myanimelist.feature.recommendation.data.repository

import androidx.work.WorkInfo
import com.gokova.myanimelist.core.database.dao.RecommendationDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.feature.recommendation.data.mapper.RecommendationMapper
import com.gokova.myanimelist.feature.recommendation.data.work.RecommendationScheduler
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationConstants.MIN_USER_LIST_THRESHOLD
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationEngineState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime
import com.gokova.myanimelist.feature.recommendation.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecommendationRepositoryImpl
    @Inject
    constructor(
        private val recommendationDao: RecommendationDao,
        private val userAnimeListDao: UserAnimeListDao,
        private val scheduler: RecommendationScheduler,
    ) : RecommendationRepository {
        override fun observeRecommendations(
            type: RecommendationType,
        ): Flow<List<RecommendedAnime>> {
            val sourceFlow =
                when (type) {
                    RecommendationType.GENRE ->
                        recommendationDao.observeRecommendationsByGenre()
                    RecommendationType.THEME ->
                        recommendationDao.observeRecommendationsByTheme()
                    RecommendationType.NEW_SEASONS ->
                        kotlinx.coroutines.flow.flowOf(emptyList())
                }
            return sourceFlow.map { list ->
                list.map { RecommendationMapper.toDomain(it) }
            }
        }

        override fun observeEngineState(): Flow<RecommendationEngineState> =
            combine(
                userAnimeListDao.observeAllUserAnime(),
                recommendationDao.observeRecommendationCount(),
                scheduler.observeFetchWorkInfo(),
            ) { userList, recCount, workInfos ->
                val activeOrFinished =
                    workInfos.filter { it.state != WorkInfo.State.CANCELLED }
                val isWorking =
                    activeOrFinished.any {
                        it.state == WorkInfo.State.RUNNING ||
                            it.state == WorkInfo.State.ENQUEUED ||
                            it.state == WorkInfo.State.BLOCKED
                    }
                val hasFailed =
                    activeOrFinished.any { it.state == WorkInfo.State.FAILED }
                val hasSucceeded =
                    activeOrFinished.isNotEmpty() &&
                        activeOrFinished.all { it.state == WorkInfo.State.SUCCEEDED }

                when {
                    userList.size < MIN_USER_LIST_THRESHOLD ->
                        RecommendationEngineState.EmptyInsufficientData
                    recCount > 0 ->
                        RecommendationEngineState.Ready(recCount)
                    isWorking ->
                        RecommendationEngineState.Calculating
                    hasFailed ->
                        RecommendationEngineState.Error("Failed to calculate recommendations")
                    hasSucceeded ->
                        RecommendationEngineState.EmptyAllCaughtUp
                    else ->
                        RecommendationEngineState.Calculating
                }
            }

        override suspend fun scheduleInitialOrPeriodicWork() {
            scheduler.schedulePeriodicWork()
            val currentRecCount = recommendationDao.getRecommendationCount()
            val userAnimeCount = userAnimeListDao.getAllUserAnime().size
            val activeOrFinished =
                scheduler
                    .observeFetchWorkInfo()
                    .first()
                    .filter { it.state != WorkInfo.State.CANCELLED }
            val hasCompletedWork =
                activeOrFinished.isNotEmpty() &&
                    activeOrFinished.all { it.state == WorkInfo.State.SUCCEEDED }
            val shouldScheduleInitial =
                currentRecCount == 0 &&
                    !hasCompletedWork &&
                    userAnimeCount >= MIN_USER_LIST_THRESHOLD
            if (shouldScheduleInitial) {
                scheduler.scheduleInitialCalculation()
            }
        }

        override suspend fun triggerImmediateEvaluation() {
            scheduler.triggerImmediateCalculation()
        }
    }
