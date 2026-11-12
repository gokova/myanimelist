package com.gokova.myanimelist.feature.franchise.presentation

import androidx.annotation.StringRes

sealed interface FranchiseUiEvent {
    data class NavigateToDetails(
        val animeId: Long,
    ) : FranchiseUiEvent

    data object NavigateBack : FranchiseUiEvent

    data class ShowSnackbar(
        @StringRes val messageRes: Int,
        val isError: Boolean = false,
    ) : FranchiseUiEvent
}
