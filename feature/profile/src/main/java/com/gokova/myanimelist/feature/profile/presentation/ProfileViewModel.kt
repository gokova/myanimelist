package com.gokova.myanimelist.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.domain.model.ImportIgnoredAnimeResult
import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.domain.usecase.ExportIgnoredAnimeUseCase
import com.gokova.myanimelist.core.domain.usecase.ImportIgnoredAnimeUseCase
import com.gokova.myanimelist.core.domain.usecase.ObserveIgnoredAnimeCountUseCase
import com.gokova.myanimelist.core.domain.usecase.ObserveUserProfileUseCase
import com.gokova.myanimelist.core.domain.usecase.RefreshUserProfileUseCase
import com.gokova.myanimelist.core.network.di.IoDispatcher
import com.gokova.myanimelist.feature.profile.R
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
class ProfileViewModel
    @Inject
    constructor(
        private val observeUserProfileUseCase: ObserveUserProfileUseCase,
        private val refreshUserProfileUseCase: RefreshUserProfileUseCase,
        observeIgnoredAnimeCountUseCase: ObserveIgnoredAnimeCountUseCase,
        private val exportIgnoredAnimeUseCase: ExportIgnoredAnimeUseCase,
        private val importIgnoredAnimeUseCase: ImportIgnoredAnimeUseCase,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
                observeIgnoredAnimeCountUseCase(),
                isRefreshing,
                showLogoutDialog,
                errorMessageResId,
            ) {
                profile: UserProfile?,
                ignoredCount: Int,
                refreshing: Boolean,
                logoutDialog: Boolean,
                errorRes: Int?,
                ->
                when {
                    profile != null ->
                        ProfileUiState.Content(
                            userProfile = profile,
                            ignoredAnimeCount = ignoredCount,
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

        fun exportIgnoredAnime(openOutputStream: () -> OutputStream?) {
            viewModelScope.launch(ioDispatcher) {
                val stream = runCatching { openOutputStream() }.getOrNull()
                if (stream == null) {
                    eventChannel.send(
                        ProfileUiEvent.ShowErrorSnackbar(R.string.profile_ignored_export_failed),
                    )
                    return@launch
                }

                val result = exportIgnoredAnimeUseCase(stream)
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

                when (val result = importIgnoredAnimeUseCase(stream)) {
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
