package com.gokova.myanimelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.feature.auth.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        authPreferences: AuthPreferences,
        private val logoutUseCase: LogoutUseCase,
    ) : ViewModel() {
        val isLoggedIn: StateFlow<Boolean?> =
            authPreferences.isLoggedIn
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = null,
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
