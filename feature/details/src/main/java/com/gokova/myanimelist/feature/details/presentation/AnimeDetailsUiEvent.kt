package com.gokova.myanimelist.feature.details.presentation

import androidx.annotation.StringRes

sealed interface AnimeDetailsUiEvent {
    data class ShowSnackbar(
        @StringRes val messageRes: Int,
        val isError: Boolean = false,
    ) : AnimeDetailsUiEvent

    data class NavigateToDetails(
        val animeId: Long,
    ) : AnimeDetailsUiEvent

    data object NavigateBack : AnimeDetailsUiEvent
}
