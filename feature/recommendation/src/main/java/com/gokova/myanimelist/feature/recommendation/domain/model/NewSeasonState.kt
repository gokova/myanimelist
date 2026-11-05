package com.gokova.myanimelist.feature.recommendation.domain.model

sealed interface NewSeasonState {
    data object EmptyInsufficientData : NewSeasonState

    data object EmptyAllCaughtUp : NewSeasonState

    data object Calculating : NewSeasonState

    data class Ready(
        val count: Int,
        val isRefreshing: Boolean = false,
        val isQueued: Boolean = false,
    ) : NewSeasonState

    data class Error(
        val message: String,
    ) : NewSeasonState
}
