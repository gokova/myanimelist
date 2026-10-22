package com.gokova.myanimelist.feature.recommendation.presentation

sealed interface RecommendationSideEffect {
    data class ShowUndoNotInterestedSnackbar(
        val animeId: Long,
        val animeTitle: String,
    ) : RecommendationSideEffect
}
