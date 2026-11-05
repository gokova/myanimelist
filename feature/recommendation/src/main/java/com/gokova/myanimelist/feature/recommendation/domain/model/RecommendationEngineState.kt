package com.gokova.myanimelist.feature.recommendation.domain.model

sealed interface RecommendationEngineState {
    data object EmptyInsufficientData : RecommendationEngineState

    data object EmptyAllCaughtUp : RecommendationEngineState

    data object Calculating : RecommendationEngineState

    data class Ready(
        val totalCount: Int,
        val isRefreshing: Boolean = false,
        val isQueued: Boolean = false,
    ) : RecommendationEngineState

    data class Error(
        val message: String,
    ) : RecommendationEngineState
}
