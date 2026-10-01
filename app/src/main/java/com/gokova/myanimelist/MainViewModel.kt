package com.gokova.myanimelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.domain.usecase.ObserveUserProfileUseCase
import com.gokova.myanimelist.core.domain.usecase.RefreshUserProfileUseCase
import com.gokova.myanimelist.feature.auth.domain.usecase.LogoutUseCase
import com.gokova.myanimelist.feature.auth.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Authenticated(
        val isLoggedIn: Boolean,
        val avatarUrl: String? = null,
    ) : MainUiState
}

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        observeAuthStateUseCase: ObserveAuthStateUseCase,
        private val logoutUseCase: LogoutUseCase,
        observeUserProfileUseCase: ObserveUserProfileUseCase,
        private val refreshUserProfileUseCase: RefreshUserProfileUseCase,
    ) : ViewModel() {
        val uiState: StateFlow<MainUiState> =
            combine(
                observeAuthStateUseCase(),
                observeUserProfileUseCase(),
            ) { isLoggedIn, profile ->
                MainUiState.Authenticated(
                    isLoggedIn = isLoggedIn,
                    avatarUrl = if (isLoggedIn) profile?.pictureUrl else null,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = MainUiState.Loading,
            )

        private var refreshJob: Job? = null

        init {
            viewModelScope.launch {
                var wasLoggedIn = false
                observeAuthStateUseCase().collect { isLoggedIn ->
                    if (isLoggedIn) {
                        wasLoggedIn = true
                        refreshJob?.cancel()
                        refreshJob =
                            launch {
                                refreshUserProfileUseCase()
                            }
                    } else if (wasLoggedIn) {
                        wasLoggedIn = false
                        refreshJob?.cancel()
                    }
                }
            }
        }

        fun refreshProfile() {
            refreshJob?.cancel()
            refreshJob =
                viewModelScope.launch {
                    refreshUserProfileUseCase()
                }
        }

        fun logout() {
            refreshJob?.cancel()
            viewModelScope.launch {
                logoutUseCase()
            }
        }

        private companion object {
            private const val STOP_TIMEOUT_MILLIS = 5000L
        }
    }
