package com.gokova.myanimelist.feature.recommendation.domain.usecase

import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationLabConfig
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.repository.RecommendationRepository
import javax.inject.Inject

class CalculateLabMatchCountUseCase
    @Inject
    constructor(
        private val repository: RecommendationRepository,
    ) {
        suspend operator fun invoke(
            type: RecommendationType,
            config: RecommendationLabConfig,
        ): Int = repository.calculateLabMatchCount(type, config)
    }
