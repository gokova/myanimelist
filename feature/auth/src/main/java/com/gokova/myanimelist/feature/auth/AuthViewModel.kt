package com.gokova.myanimelist.feature.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.feature.auth.domain.usecase.GetAuthUrlUseCase
import com.gokova.myanimelist.feature.auth.domain.usecase.LoginWithCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val getAuthUrlUseCase: GetAuthUrlUseCase,
        private val loginWithCodeUseCase: LoginWithCodeUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        fun generateAuthUrl(): Uri =
            runBlocking {
                getAuthUrlUseCase()
            }

        fun handleAuthorizationCode(code: String) {
            viewModelScope.launch {
                _uiState.value = AuthUiState.Loading

                val result = loginWithCodeUseCase(code)
                if (result.isSuccess) {
                    _uiState.value = AuthUiState.Success
                } else {
                    _uiState.value = AuthUiState.Error("Failed to login. Please try again.")
                }
            }
        }
    }

sealed interface AuthUiState {
    data object Idle : AuthUiState

    data object Loading : AuthUiState

    data object Success : AuthUiState

    data class Error(
        val message: String,
    ) : AuthUiState
}
