package com.gokova.myanimelist.feature.recommendation.presentation

import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType

sealed interface RecommendationUiEvent {
    data class SelectType(
        val type: RecommendationType,
    ) : RecommendationUiEvent

    data class SelectNewSeasonSort(
        val sort: NewSeasonSortOption,
    ) : RecommendationUiEvent

    data object CalculateNow : RecommendationUiEvent

    data object Refresh : RecommendationUiEvent
}
