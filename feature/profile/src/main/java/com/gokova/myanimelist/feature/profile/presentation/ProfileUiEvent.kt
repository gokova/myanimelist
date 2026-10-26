package com.gokova.myanimelist.feature.profile.presentation

import androidx.annotation.StringRes

sealed interface ProfileUiEvent {
    data object NavigateBack : ProfileUiEvent

    data object RequestLogout : ProfileUiEvent

    data class ShowErrorSnackbar(
        @StringRes val messageResId: Int,
    ) : ProfileUiEvent

    data class ShowExportSuccessSnackbar(
        val count: Int,
    ) : ProfileUiEvent

    data class ShowImportSuccessSnackbar(
        val totalCount: Int,
        val newCount: Int,
        val updatedCount: Int,
    ) : ProfileUiEvent
}
