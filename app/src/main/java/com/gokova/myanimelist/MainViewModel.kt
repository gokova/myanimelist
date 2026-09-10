package com.gokova.myanimelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.feature.auth.domain.usecase.LogoutUseCase
import com.gokova.myanimelist.feature.auth.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Authenticated(
        val isLoggedIn: Boolean,
    ) : MainUiState
}

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        observeAuthStateUseCase: ObserveAuthStateUseCase,
        private val logoutUseCase: LogoutUseCase,
    ) : ViewModel() {
        val uiState: StateFlow<MainUiState> =
            observeAuthStateUseCase()
                .map { isLoggedIn -> MainUiState.Authenticated(isLoggedIn) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = MainUiState.Loading,
                )

        fun logout() {
            viewModelScope.launch {
                logoutUseCase()
            }
        }

        private companion object {
            private const val STOP_TIMEOUT_MILLIS = 5000L
        }
    }
