package com.gokova.myanimelist.feature.recommendation.domain.usecase

import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime
import com.gokova.myanimelist.feature.recommendation.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRecommendationsUseCase
    @Inject
    constructor(
        private val repository: RecommendationRepository,
    ) {
        operator fun invoke(type: RecommendationType): Flow<List<RecommendedAnime>> =
            repository.observeRecommendations(type)
    }
