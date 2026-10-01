package com.gokova.myanimelist.feature.profile.presentation

import androidx.annotation.StringRes
import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.feature.profile.R

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Content(
        val userProfile: UserProfile,
        val isRefreshing: Boolean = false,
        val showLogoutDialog: Boolean = false,
    ) : ProfileUiState

    data class Error(
        @StringRes val messageResId: Int = R.string.profile_load_failed,
    ) : ProfileUiState
}
