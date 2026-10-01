package com.gokova.myanimelist.feature.profile.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gokova.myanimelist.core.domain.model.UserAnimeStatistics
import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.feature.profile.R

class ProfileUiStatePreviewParameterProvider : PreviewParameterProvider<ProfileUiState> {
    override val values: Sequence<ProfileUiState> =
        sequenceOf(
            ProfileUiState.Content(
                userProfile =
                    UserProfile(
                        id = 6548478L,
                        name = "rin-0911-3",
                        pictureUrl = "https://example.com/avatar.jpg",
                        gender = "male",
                        birthday = "1990-01-01",
                        location = "Tokyo, Japan",
                        joinedAt = "2017-09-11T10:27:46+00:00",
                        statistics =
                            UserAnimeStatistics(
                                numItemsWatching = 2,
                                numItemsCompleted = 142,
                                numItemsOnHold = 5,
                                numItemsDropped = 3,
                                numItemsPlanToWatch = 997,
                                numItems = 1149,
                                numDaysWatched = 42.5f,
                                numDaysWatching = 1.2f,
                                numDaysCompleted = 39.8f,
                                numDaysOnHold = 1.1f,
                                numDaysDropped = 0.4f,
                                numDays = 42.5f,
                                numEpisodes = 1824,
                                numTimesRewatched = 12,
                                meanScore = 7.92f,
                            ),
                    ),
            ),
            ProfileUiState.Content(
                userProfile =
                    UserProfile(
                        id = 12345L,
                        name = "minimal_user",
                        pictureUrl = null,
                        gender = null,
                        birthday = null,
                        location = null,
                        joinedAt = "2023-01-01T00:00:00+00:00",
                        statistics = null,
                    ),
            ),
            ProfileUiState.Content(
                userProfile =
                    UserProfile(
                        id = 6548478L,
                        name = "rin-0911-3",
                        pictureUrl = null,
                        gender = null,
                        birthday = null,
                        location = null,
                        joinedAt = null,
                        statistics = null,
                    ),
                showLogoutDialog = true,
            ),
            ProfileUiState.Loading,
            ProfileUiState.Error(messageResId = R.string.profile_load_failed),
        )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            INDEX_FULL -> "Full Profile"
            INDEX_MINIMAL -> "Minimal Profile"
            INDEX_LOGOUT_DIALOG -> "Logout Dialog Visible"
            INDEX_LOADING -> "Loading"
            INDEX_ERROR -> "Error"
            else -> null
        }

    private companion object {
        private const val INDEX_FULL = 0
        private const val INDEX_MINIMAL = 1
        private const val INDEX_LOGOUT_DIALOG = 2
        private const val INDEX_LOADING = 3
        private const val INDEX_ERROR = 4
    }
}
