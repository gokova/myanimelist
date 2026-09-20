package com.gokova.myanimelist.feature.recommendation.presentation

import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType

sealed interface RecommendationUiEvent {
    data class SelectType(
        val type: RecommendationType,
    ) : RecommendationUiEvent

    data object CalculateNow : RecommendationUiEvent

    data object Refresh : RecommendationUiEvent
}
