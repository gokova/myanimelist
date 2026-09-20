package com.gokova.myanimelist.feature.recommendation.presentation

import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime

sealed interface RecommendationUiState {
    data object Loading : RecommendationUiState

    data object EmptyInsufficientData : RecommendationUiState

    data object Calculating : RecommendationUiState

    data class Success(
        val selectedType: RecommendationType,
        val recommendations: List<RecommendedAnime>,
    ) : RecommendationUiState

    data class Error(
        val message: String,
    ) : RecommendationUiState
}
