package com.gokova.myanimelist.feature.profile.presentation

sealed interface ProfileUiEvent {
    data object NavigateBack : ProfileUiEvent

    data object RequestLogout : ProfileUiEvent
}
