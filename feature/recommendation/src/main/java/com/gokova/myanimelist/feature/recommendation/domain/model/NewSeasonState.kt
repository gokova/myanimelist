package com.gokova.myanimelist.feature.recommendation.domain.model

sealed interface NewSeasonState {
    data object EmptyInsufficientData : NewSeasonState

    data object EmptyAllCaughtUp : NewSeasonState

    data object Calculating : NewSeasonState

    data class Ready(
        val count: Int,
    ) : NewSeasonState

    data class Error(
        val message: String,
    ) : NewSeasonState
}
