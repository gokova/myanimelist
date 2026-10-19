package com.gokova.myanimelist.feature.details.presentation

import androidx.annotation.StringRes

sealed interface AnimeDetailsUiEvent {
    data class ShowSnackbar(
        @StringRes val messageRes: Int,
        val isError: Boolean = false,
    ) : AnimeDetailsUiEvent

    data class ShowCompletionPrompt(
        val animeId: Long,
        val title: String,
    ) : AnimeDetailsUiEvent

    data class ShowWatchingPrompt(
        val animeId: Long,
        val title: String,
    ) : AnimeDetailsUiEvent

    data class NavigateToDetails(
        val animeId: Long,
    ) : AnimeDetailsUiEvent

    data object NavigateBack : AnimeDetailsUiEvent
}
