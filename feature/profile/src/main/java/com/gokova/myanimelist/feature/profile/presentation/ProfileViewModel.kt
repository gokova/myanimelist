package com.gokova.myanimelist.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.domain.usecase.ObserveUserProfileUseCase
import com.gokova.myanimelist.core.domain.usecase.RefreshUserProfileUseCase
import com.gokova.myanimelist.feature.profile.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        observeUserProfileUseCase: ObserveUserProfileUseCase,
        private val refreshUserProfileUseCase: RefreshUserProfileUseCase,
    ) : ViewModel() {
        private val eventChannel = Channel<ProfileUiEvent>(Channel.BUFFERED)
        val uiEvent: Flow<ProfileUiEvent> = eventChannel.receiveAsFlow()

        private val isRefreshing = MutableStateFlow(false)
        private val showLogoutDialog = MutableStateFlow(false)
        private val errorMessageResId = MutableStateFlow<Int?>(null)
        private var refreshJob: Job? = null

        val uiState: StateFlow<ProfileUiState> =
            combine(
                observeUserProfileUseCase(),
                isRefreshing,
                showLogoutDialog,
                errorMessageResId,
            ) { profile: UserProfile?, refreshing: Boolean, logoutDialog: Boolean, errorRes: Int? ->
                when {
                    profile != null ->
                        ProfileUiState.Content(
                            userProfile = profile,
                            isRefreshing = refreshing,
                            showLogoutDialog = logoutDialog,
                        )
                    errorRes != null -> ProfileUiState.Error(messageResId = errorRes)
                    else -> ProfileUiState.Loading
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = ProfileUiState.Loading,
            )

        init {
            refresh()
        }

        fun refresh() {
            refreshJob?.cancel()
            refreshJob =
                viewModelScope.launch {
                    isRefreshing.update { true }
                    errorMessageResId.update { null }
                    try {
                        val result = refreshUserProfileUseCase()
                        result.onFailure {
                            errorMessageResId.update { R.string.profile_load_failed }
                        }
                    } finally {
                        isRefreshing.update { false }
                    }
                }
        }

        fun onLogoutClicked() {
            showLogoutDialog.update { true }
        }

        fun onLogoutDismissed() {
            showLogoutDialog.update { false }
        }

        fun onLogoutConfirmed() {
            refreshJob?.cancel()
            showLogoutDialog.update { false }
            viewModelScope.launch {
                eventChannel.send(ProfileUiEvent.RequestLogout)
            }
        }

        fun onBackClicked() {
            viewModelScope.launch {
                eventChannel.send(ProfileUiEvent.NavigateBack)
            }
        }

        private companion object {
            private const val STOP_TIMEOUT_MILLIS = 5000L
        }
    }
