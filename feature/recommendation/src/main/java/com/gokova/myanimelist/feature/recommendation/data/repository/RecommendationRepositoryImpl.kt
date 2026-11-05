package com.gokova.myanimelist.feature.recommendation.data.repository

import androidx.work.WorkInfo
import com.gokova.myanimelist.core.database.dao.RecommendationDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.domain.taxonomy.TagType
import com.gokova.myanimelist.feature.recommendation.data.mapper.RecommendationMapper
import com.gokova.myanimelist.feature.recommendation.data.work.RecommendationScheduler
import com.gokova.myanimelist.feature.recommendation.domain.algorithm.RecommendationScorer
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationConstants.MIN_USER_LIST_THRESHOLD
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationEngineState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationLabConfig
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime
import com.gokova.myanimelist.feature.recommendation.domain.model.UserTasteProfileItem
import com.gokova.myanimelist.feature.recommendation.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class RecommendationRepositoryImpl
    @Inject
    constructor(
        private val recommendationDao: RecommendationDao,
        private val userAnimeListDao: UserAnimeListDao,
        private val scheduler: RecommendationScheduler,
    ) : RecommendationRepository {
        private val scorer = RecommendationScorer()

        override fun observeRecommendations(
            type: RecommendationType,
            config: RecommendationLabConfig,
        ): Flow<List<RecommendedAnime>> {
            if (type == RecommendationType.NEW_SEASONS) {
                return flowOf(emptyList())
            }

            val savedFlow =
                if (type == RecommendationType.GENRE) {
                    recommendationDao.observeRecommendationsByGenre()
                } else {
                    recommendationDao.observeRecommendationsByTheme()
                }

            return combine(
                recommendationDao.observeCandidateAnimes(),
                userAnimeListDao.observeAllUserAnime(),
                savedFlow,
            ) { candidateEntities, userAnimeList, savedRecs ->
                val userTaste =
                    userAnimeList.map { item ->
                        UserTasteProfileItem(
                            animeId = item.anime.id,
                            genres = item.anime.genres?.map { it.name } ?: emptyList(),
                            userScore = item.userAnime.score.takeIf { it > 0 },
                        )
                    }

                val userAnimeIds = userAnimeList.map { it.anime.id }.toSet()
                val pool =
                    (candidateEntities + savedRecs.map { it.anime })
                        .distinctBy { it.id }
                        .filter { it.id !in userAnimeIds }

                if (pool.isEmpty() || userTaste.isEmpty()) {
                    return@combine savedRecs.map { RecommendationMapper.toDomain(it) }
                }

                val targetType =
                    if (type == RecommendationType.GENRE) TagType.GENRE else TagType.THEME
                val candidates =
                    pool.map { RecommendationMapper.toCandidateAnimeItem(it) }
                val labResults = scorer.evaluateLab(userTaste, candidates, config, targetType)

                val entityMap = pool.associateBy { it.id }
                val results =
                    labResults.mapNotNull { result ->
                        val entity = entityMap[result.animeId] ?: return@mapNotNull null
                        RecommendationMapper.toRecommendedAnime(entity, result, type)
                    }

                if (results.isEmpty() && config.isDefault) {
                    savedRecs.map { RecommendationMapper.toDomain(it) }
                } else {
                    results
                }
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
                val isRunning = activeOrFinished.any { it.state == WorkInfo.State.RUNNING }
                val isQueued =
                    activeOrFinished.any {
                        it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED
                    }
                val isWorking = isRunning || isQueued
                val hasFailed =
                    activeOrFinished.any { it.state == WorkInfo.State.FAILED }
                val hasSucceeded =
                    activeOrFinished.isNotEmpty() &&
                        activeOrFinished.all { it.state == WorkInfo.State.SUCCEEDED }

                when {
                    userList.size < MIN_USER_LIST_THRESHOLD ->
                        RecommendationEngineState.EmptyInsufficientData
                    recCount > 0 ->
                        RecommendationEngineState.Ready(
                            totalCount = recCount,
                            isRefreshing = isRunning,
                            isQueued = isQueued,
                        )
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

        override suspend fun calculateLabMatchCount(
            type: RecommendationType,
            config: RecommendationLabConfig,
        ): Int {
            if (type == RecommendationType.NEW_SEASONS) return 0
            val userAnimeList = userAnimeListDao.getAllUserAnime()
            val userList =
                userAnimeList.map { item ->
                    UserTasteProfileItem(
                        animeId = item.anime.id,
                        genres = item.anime.genres?.map { it.name } ?: emptyList(),
                        userScore = item.userAnime.score.takeIf { it > 0 },
                    )
                }
            val savedRecs =
                if (type == RecommendationType.GENRE) {
                    recommendationDao.observeRecommendationsByGenre().first()
                } else {
                    recommendationDao.observeRecommendationsByTheme().first()
                }
            val userAnimeIds = userAnimeList.map { it.anime.id }.toSet()
            val pool =
                (recommendationDao.getCandidateAnimes() + savedRecs.map { it.anime })
                    .distinctBy { it.id }
                    .filter { it.id !in userAnimeIds }

            return when {
                userList.isEmpty() || pool.isEmpty() -> 0
                else -> {
                    val targetType =
                        if (type == RecommendationType.GENRE) TagType.GENRE else TagType.THEME
                    val candidates = pool.map { RecommendationMapper.toCandidateAnimeItem(it) }
                    val results = scorer.evaluateLab(userList, candidates, config, targetType)
                    if (results.isEmpty() && config.isDefault) savedRecs.size else results.size
                }
            }
        }
    }
