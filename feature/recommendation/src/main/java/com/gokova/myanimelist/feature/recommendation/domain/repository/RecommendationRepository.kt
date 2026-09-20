package com.gokova.myanimelist.feature.recommendation.domain.repository

import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationEngineState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime
import kotlinx.coroutines.flow.Flow

interface RecommendationRepository {
    fun observeRecommendations(type: RecommendationType): Flow<List<RecommendedAnime>>

    fun observeEngineState(): Flow<RecommendationEngineState>

    suspend fun scheduleInitialOrPeriodicWork()

    suspend fun triggerImmediateEvaluation()
}
