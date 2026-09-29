package com.gokova.myanimelist.feature.details.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.feature.details.R
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.domain.usecase.AddAnimeToMyListUseCase
import com.gokova.myanimelist.feature.details.domain.usecase.ObserveAnimeDetailsUseCase
import com.gokova.myanimelist.feature.details.domain.usecase.RefreshAnimeDetailsUseCase
import com.gokova.myanimelist.feature.details.navigation.AnimeDetailsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimeDetailsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val observeAnimeDetailsUseCase: ObserveAnimeDetailsUseCase,
        private val refreshAnimeDetailsUseCase: RefreshAnimeDetailsUseCase,
        private val addAnimeToMyListUseCase: AddAnimeToMyListUseCase,
    ) : ViewModel() {
        val animeId: Long =
            runCatching { savedStateHandle.toRoute<AnimeDetailsRoute>().animeId }
                .getOrNull() ?: (savedStateHandle.get<Long>("animeId") ?: 0L)

        private val _uiState = MutableStateFlow(AnimeDetailsUiState())
        val uiState: StateFlow<AnimeDetailsUiState> = _uiState.asStateFlow()

        private val eventChannel = Channel<AnimeDetailsUiEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()

        init {
            observeLocalDetails()
            refresh(isInitial = true)
        }

        private fun observeLocalDetails() {
            viewModelScope.launch {
                observeAnimeDetailsUseCase(animeId).collect { localDetails ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = current.details == null && localDetails == null,
                            details = mergeDetails(current.details, localDetails),
                        )
                    }
                }
            }
        }

        fun onRefresh() {
            refresh(isInitial = false)
        }

        fun onRetry() {
            refresh(isInitial = true)
        }

        private fun refresh(isInitial: Boolean) {
            viewModelScope.launch {
                val hasCachedDetails = _uiState.value.details != null
                _uiState.update { current ->
                    current.copy(
                        isLoading = !hasCachedDetails,
                        isRefreshing = hasCachedDetails && !isInitial,
                        errorMessageRes = null,
                        canRetry = false,
                    )
                }

                val result = refreshAnimeDetailsUseCase(animeId)
                result.fold(
                    onSuccess = { freshDetails ->
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                isRefreshing = false,
                                details = mergeDetails(current.details, freshDetails),
                                errorMessageRes = null,
                                canRetry = false,
                            )
                        }
                    },
                    onFailure = { error ->
                        AppLog.ui.w(
                            error,
                        ) { "Failed refreshing anime details for animeId=$animeId" }
                        val currentDetails = _uiState.value.details
                        if (currentDetails != null) {
                            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                            eventChannel.send(
                                AnimeDetailsUiEvent.ShowSnackbar(
                                    messageRes = R.string.details_offline_showing_cached,
                                    isError = false,
                                ),
                            )
                        } else {
                            _uiState.update { current ->
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    errorMessageRes = R.string.details_error_not_found,
                                    canRetry = true,
                                )
                            }
                        }
                    },
                )
            }
        }

        fun onAddToList() {
            if (_uiState.value.isAddingToList || _uiState.value.details?.isInUserList == true) {
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isAddingToList = true) }
                val result = addAnimeToMyListUseCase(animeId)
                result.fold(
                    onSuccess = {
                        _uiState.update { current ->
                            current.copy(
                                isAddingToList = false,
                                details =
                                    current.details?.copy(
                                        isInUserList = true,
                                        userStatus = "plan_to_watch",
                                    ),
                            )
                        }
                        eventChannel.send(
                            AnimeDetailsUiEvent.ShowSnackbar(
                                messageRes = R.string.details_added_to_plan_to_watch,
                                isError = false,
                            ),
                        )
                    },
                    onFailure = {
                        _uiState.update { it.copy(isAddingToList = false) }
                        eventChannel.send(
                            AnimeDetailsUiEvent.ShowSnackbar(
                                messageRes = R.string.details_add_to_list_failed,
                                isError = true,
                            ),
                        )
                    },
                )
            }
        }

        fun onAnimeClick(targetAnimeId: Long) {
            viewModelScope.launch {
                eventChannel.send(AnimeDetailsUiEvent.NavigateToDetails(targetAnimeId))
            }
        }

        fun onBackClick() {
            viewModelScope.launch {
                eventChannel.send(AnimeDetailsUiEvent.NavigateBack)
            }
        }

        private fun mergeDetails(
            current: AnimeDetails?,
            incoming: AnimeDetails?,
        ): AnimeDetails? =
            when {
                incoming == null -> current
                current == null -> incoming
                else ->
                    incoming.copy(
                        relatedAnime =
                            incoming.relatedAnime.ifEmpty {
                                current.relatedAnime
                            },
                        recommendations =
                            incoming.recommendations.ifEmpty {
                                current.recommendations
                            },
                        numScoringUsers = incoming.numScoringUsers ?: current.numScoringUsers,
                        subtitleTitle = incoming.subtitleTitle ?: current.subtitleTitle,
                        originalTitle =
                            incoming.originalTitle.ifBlank {
                                current.originalTitle
                            },
                    )
            }
    }
