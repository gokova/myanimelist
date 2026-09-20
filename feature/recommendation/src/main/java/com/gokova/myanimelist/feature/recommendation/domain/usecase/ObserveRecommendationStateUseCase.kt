package com.gokova.myanimelist.feature.recommendation.domain.usecase

import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationEngineState
import com.gokova.myanimelist.feature.recommendation.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRecommendationStateUseCase
    @Inject
    constructor(
        private val repository: RecommendationRepository,
    ) {
        operator fun invoke(): Flow<RecommendationEngineState> = repository.observeEngineState()
    }
