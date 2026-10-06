package com.gokova.myanimelist.feature.search.presentation

import androidx.annotation.StringRes

sealed interface SearchUiEvent {
    data class ShowSnackbar(
        @StringRes val messageRes: Int,
    ) : SearchUiEvent
}
