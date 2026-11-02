package com.gokova.myanimelist.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.ai.model.AiException
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.domain.model.ImportIgnoredAnimeResult
import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.domain.usecase.IgnoredAnimeBackupInteractor
import com.gokova.myanimelist.core.domain.usecase.ObserveUserProfileUseCase
import com.gokova.myanimelist.core.domain.usecase.RefreshUserProfileUseCase
import com.gokova.myanimelist.core.network.di.IoDispatcher
import com.gokova.myanimelist.feature.profile.R
import com.gokova.myanimelist.feature.profile.domain.usecase.ManageAiApiKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class ProfileViewModel
    @Inject
    constructor(
        private val observeUserProfileUseCase: ObserveUserProfileUseCase,
        private val refreshUserProfileUseCase: RefreshUserProfileUseCase,
        private val backupInteractor: IgnoredAnimeBackupInteractor,
        private val manageAiApiKeyUseCase: ManageAiApiKeyUseCase,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val eventChannel = Channel<ProfileUiEvent>(Channel.BUFFERED)
        val uiEvent: Flow<ProfileUiEvent> = eventChannel.receiveAsFlow()

        private val isRefreshing = MutableStateFlow(false)
        private val showLogoutDialog = MutableStateFlow(false)
        private val showAiInfoDialog = MutableStateFlow(false)
        private val aiKeyValidationStatus =
            MutableStateFlow(AiKeyValidationStatus.Idle)
        private val errorMessageResId = MutableStateFlow<Int?>(null)
        private var refreshJob: Job? = null

        val uiState: StateFlow<ProfileUiState> =
            combine(
                observeUserProfileUseCase(),
                backupInteractor.observeCount(),
                manageAiApiKeyUseCase.apiKey,
                isRefreshing,
                showLogoutDialog,
                aiKeyValidationStatus,
                showAiInfoDialog,
                errorMessageResId,
            ) { args: Array<Any?> ->
                val profile = args[0] as? UserProfile
                val ignoredCount = args[1] as Int
                val aiApiKey = args[2] as? String
                val refreshing = args[3] as Boolean
                val logoutDialog = args[4] as Boolean
                val validationStatus = args[5] as AiKeyValidationStatus
                val aiInfo = args[6] as Boolean
                val errorRes = args[7] as? Int

                when {
                    profile != null ->
                        ProfileUiState.Content(
                            userProfile = profile,
                            ignoredAnimeCount = ignoredCount,
                            isRefreshing = refreshing,
                            showLogoutDialog = logoutDialog,
                            aiApiKey = aiApiKey,
                            aiKeyValidationStatus = validationStatus,
                            showAiInfoDialog = aiInfo,
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
                            if (observeUserProfileUseCase().first() != null) {
                                eventChannel.send(
                                    ProfileUiEvent.ShowErrorSnackbar(R.string.profile_load_failed),
                                )
                            }
                        }
                    } finally {
                        isRefreshing.update { false }
                    }
                }
        }

        fun setLogoutDialogVisible(visible: Boolean) {
            showLogoutDialog.update { visible }
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

        fun saveAiApiKey(key: String) {
            viewModelScope.launch {
                manageAiApiKeyUseCase.saveKey(key)
                aiKeyValidationStatus.update { AiKeyValidationStatus.Idle }
            }
        }

        fun clearAiApiKey() {
            viewModelScope.launch {
                manageAiApiKeyUseCase.clearKey()
                aiKeyValidationStatus.update { AiKeyValidationStatus.Idle }
            }
        }

        fun testAiApiKey(key: String) {
            viewModelScope.launch {
                aiKeyValidationStatus.update { AiKeyValidationStatus.Testing }
                val result = manageAiApiKeyUseCase.testKey(key)
                aiKeyValidationStatus.update {
                    if (result.isSuccess) {
                        AppLog.domain.i { "AI API key test succeeded" }
                        AiKeyValidationStatus.Valid
                    } else {
                        val error = result.exceptionOrNull()
                        AppLog.domain.e(error) { "AI API key test failed" }
                        when (error) {
                            is AiException.QuotaExceeded -> AiKeyValidationStatus.QuotaExceeded
                            is AiException.NetworkError -> {
                                if (error.message?.contains("503") == true) {
                                    AiKeyValidationStatus.ServiceUnavailable
                                } else {
                                    AiKeyValidationStatus.NetworkError
                                }
                            }
                            else -> AiKeyValidationStatus.Invalid
                        }
                    }
                }
            }
        }

        fun setAiInfoDialogVisible(visible: Boolean) {
            showAiInfoDialog.update { visible }
        }

        fun exportIgnoredAnime(openOutputStream: () -> OutputStream?) {
            viewModelScope.launch(ioDispatcher) {
                val stream = runCatching { openOutputStream() }.getOrNull()
                if (stream == null) {
                    eventChannel.send(
                        ProfileUiEvent.ShowErrorSnackbar(R.string.profile_ignored_export_failed),
                    )
                    return@launch
                }

                val result = backupInteractor.export(stream)
                result.fold(
                    onSuccess = { count ->
                        eventChannel.send(ProfileUiEvent.ShowExportSuccessSnackbar(count))
                    },
                    onFailure = {
                        eventChannel.send(
                            ProfileUiEvent.ShowErrorSnackbar(
                                R.string.profile_ignored_export_failed,
                            ),
                        )
                    },
                )
            }
        }

        fun importIgnoredAnime(openInputStream: () -> InputStream?) {
            viewModelScope.launch(ioDispatcher) {
                val stream = runCatching { openInputStream() }.getOrNull()
                if (stream == null) {
                    eventChannel.send(
                        ProfileUiEvent.ShowErrorSnackbar(R.string.profile_ignored_import_failed),
                    )
                    return@launch
                }

                when (val result = backupInteractor.import(stream)) {
                    is ImportIgnoredAnimeResult.Success -> {
                        eventChannel.send(
                            ProfileUiEvent.ShowImportSuccessSnackbar(
                                totalCount = result.totalCount,
                                newCount = result.newCount,
                                updatedCount = result.updatedCount,
                            ),
                        )
                    }
                    is ImportIgnoredAnimeResult.Failure.EmptyFile -> {
                        eventChannel.send(
                            ProfileUiEvent.ShowErrorSnackbar(R.string.profile_ignored_import_empty),
                        )
                    }
                    is ImportIgnoredAnimeResult.Failure.InvalidFormat -> {
                        eventChannel.send(
                            ProfileUiEvent.ShowErrorSnackbar(
                                R.string.profile_ignored_import_invalid,
                            ),
                        )
                    }
                    is ImportIgnoredAnimeResult.Failure.IoError -> {
                        eventChannel.send(
                            ProfileUiEvent.ShowErrorSnackbar(
                                R.string.profile_ignored_import_failed,
                            ),
                        )
                    }
                }
            }
        }

        private companion object {
            private const val STOP_TIMEOUT_MILLIS = 5000L
        }
    }
