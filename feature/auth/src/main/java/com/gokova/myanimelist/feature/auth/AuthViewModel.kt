package com.gokova.myanimelist.feature.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.feature.auth.domain.usecase.GetAuthUrlUseCase
import com.gokova.myanimelist.feature.auth.domain.usecase.LoginWithCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiEvent {
    data class OpenOAuthUrl(
        val url: String,
    ) : AuthUiEvent

    data object AuthSuccess : AuthUiEvent
}

sealed interface AuthUiState {
    data object Idle : AuthUiState

    data object Loading : AuthUiState

    data class Error(
        @StringRes val messageResId: Int,
    ) : AuthUiState
}

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val getAuthUrlUseCase: GetAuthUrlUseCase,
        private val loginWithCodeUseCase: LoginWithCodeUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        private val _events = Channel<AuthUiEvent>(Channel.BUFFERED)
        val events: Flow<AuthUiEvent> = _events.receiveAsFlow()

        fun onLoginClicked() {
            AppLog.ui.i { "User clicked login button" }
            viewModelScope.launch {
                _uiState.value = AuthUiState.Loading
                AppLog.viewModel.d { "Transitioned state to Loading; requesting auth URL" }
                try {
                    val authUrl = getAuthUrlUseCase()
                    _events.send(AuthUiEvent.OpenOAuthUrl(authUrl))
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.viewModel.e(e) { "Failed to retrieve OAuth authorization URL" }
                    _uiState.value = AuthUiState.Error(R.string.feature_auth_error_login_failed)
                }
            }
        }

        fun handleAuthorizationCode(
            code: String,
            state: String? = null,
        ) {
            AppLog.viewModel.i {
                "Handling authorization code (codeLength=${code.length}, hasState=${state != null})"
            }
            viewModelScope.launch {
                _uiState.value = AuthUiState.Loading
                try {
                    val result = loginWithCodeUseCase(code, state)
                    if (result.isSuccess) {
                        AppLog.viewModel.i { "Login succeeded, emitting AuthSuccess event" }
                        _events.send(AuthUiEvent.AuthSuccess)
                        _uiState.value = AuthUiState.Idle
                    } else {
                        AppLog.viewModel.w { "Login with authorization code failed" }
                        _uiState.value = AuthUiState.Error(R.string.feature_auth_error_login_failed)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.viewModel.e(e) { "Exception occurred during authorization code login" }
                    _uiState.value = AuthUiState.Error(R.string.feature_auth_error_login_failed)
                }
            }
        }

        fun handleRedirectError(error: String) {
            AppLog.viewModel.w { "OAuth redirect error received: $error" }
            if (error == "access_denied") {
                // User canceled or denied authorization in browser - return gracefully to Idle
                _uiState.value = AuthUiState.Idle
            } else {
                _uiState.value = AuthUiState.Error(R.string.feature_auth_error_login_failed)
            }
        }
    }
