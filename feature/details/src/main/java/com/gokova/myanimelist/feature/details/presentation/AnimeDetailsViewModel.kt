package com.gokova.myanimelist.feature.details.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.usecase.IgnoredAnimeInteractor
import com.gokova.myanimelist.core.ui.component.editlist.EditAnimeStatusSheetState
import com.gokova.myanimelist.feature.details.R
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.domain.usecase.AddAnimeToMyListUseCase
import com.gokova.myanimelist.feature.details.domain.usecase.AnimeStatusInteractor
import com.gokova.myanimelist.feature.details.domain.usecase.ObserveAnimeDetailsUseCase
import com.gokova.myanimelist.feature.details.domain.usecase.RefreshAnimeDetailsUseCase
import com.gokova.myanimelist.feature.details.navigation.AnimeDetailsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.gokova.myanimelist.core.ui.R as CoreUiR

@HiltViewModel
@Suppress("TooManyFunctions")
class AnimeDetailsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val observeAnimeDetailsUseCase: ObserveAnimeDetailsUseCase,
        private val refreshAnimeDetailsUseCase: RefreshAnimeDetailsUseCase,
        private val addAnimeToMyListUseCase: AddAnimeToMyListUseCase,
        private val statusInteractor: AnimeStatusInteractor,
        private val ignoredAnimeInteractor: IgnoredAnimeInteractor,
    ) : ViewModel() {
        val animeId: Long =
            runCatching { savedStateHandle.toRoute<AnimeDetailsRoute>().animeId }
                .getOrNull() ?: (savedStateHandle.get<Long>("animeId") ?: 0L)

        private val _uiState = MutableStateFlow(AnimeDetailsUiState())
        val uiState: StateFlow<AnimeDetailsUiState> = _uiState.asStateFlow()

        private val eventChannel = Channel<AnimeDetailsUiEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()

        private var toggleNotInterestedJob: Job? = null

        init {
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
            viewModelScope.launch {
                ignoredAnimeInteractor.observeIsIgnored(animeId).collect { isIgnored ->
                    _uiState.update { it.copy(isNotInterested = isIgnored) }
                }
            }
            onRefresh(isInitial = true)
        }

        fun onRefresh(isInitial: Boolean = false) {
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

        fun onRetry() {
            onRefresh(isInitial = true)
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
                                isNotInterested = false,
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

        fun onToggleNotInterested() {
            val current = _uiState.value
            val details = current.details
            if (toggleNotInterestedJob?.isActive == true ||
                details == null ||
                details.isInUserList
            ) {
                return
            }
            val isCurrentlyIgnored = current.isNotInterested
            _uiState.update { it.copy(isNotInterested = !isCurrentlyIgnored) }
            toggleNotInterestedJob =
                viewModelScope.launch {
                    try {
                        if (isCurrentlyIgnored) {
                            ignoredAnimeInteractor.removeNotInterested(animeId)
                            eventChannel.send(
                                AnimeDetailsUiEvent.ShowSnackbar(
                                    messageRes = R.string.details_snackbar_removed_not_interested,
                                    isError = false,
                                ),
                            )
                        } else {
                            ignoredAnimeInteractor.setNotInterested(
                                animeId = animeId,
                                title = details.displayTitle,
                                titleEnglish = details.englishTitle,
                                pictureUrl = details.mainPictureMedium,
                            )
                            eventChannel.send(
                                AnimeDetailsUiEvent.ShowSnackbar(
                                    messageRes = R.string.details_snackbar_marked_not_interested,
                                    isError = false,
                                ),
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        AppLog.ui.w(e) { "Failed toggling not interested for animeId=$animeId" }
                        _uiState.update { it.copy(isNotInterested = isCurrentlyIgnored) }
                    }
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

        fun setEditingSheet(open: Boolean) {
            if (open) {
                val current = _uiState.value.details ?: return
                val userStatus = current.userStatus ?: "plan_to_watch"
                _uiState.update { state ->
                    state.copy(
                        editingSheetState =
                            EditAnimeStatusSheetState(
                                animeId = current.id,
                                title = current.displayTitle,
                                status = userStatus,
                                episodesWatched = current.numWatchedEpisodes,
                                totalEpisodes = current.numEpisodes,
                                score = current.userScore ?: 0,
                                isRewatching = current.isRewatching,
                                initialStatus = userStatus,
                                initialEpisodesWatched = current.numWatchedEpisodes,
                            ),
                    )
                }
            } else {
                if (_uiState.value.editingSheetState?.isMutating == true) return
                _uiState.update { it.copy(editingSheetState = null) }
            }
        }

        fun updateEditingSheet(
            transform: (EditAnimeStatusSheetState) -> EditAnimeStatusSheetState,
        ) {
            _uiState.update { current ->
                val sheet = current.editingSheetState
                if (sheet == null || sheet.isMutating) {
                    current
                } else {
                    current.copy(editingSheetState = transform(sheet))
                }
            }
        }

        fun onSaveEditSheet() {
            val sheet = _uiState.value.editingSheetState ?: return
            if (sheet.isMutating) return
            _uiState.update { current ->
                current.copy(
                    editingSheetState = current.editingSheetState?.copy(isSaving = true),
                )
            }
            viewModelScope.launch {
                val params =
                    UpdateUserAnimeStatusParams(
                        status = sheet.status,
                        numWatchedEpisodes = sheet.episodesWatched,
                        score = sheet.score,
                        isRewatching = sheet.isRewatching,
                    )
                val result = statusInteractor.updateStatus(sheet.animeId, params)
                result.fold(
                    onSuccess = {
                        _uiState.update { current ->
                            current.copy(
                                editingSheetState = null,
                                details = applySavedSheetToDetails(current.details, sheet),
                            )
                        }
                        resolveDetailsPromptEvent(sheet)?.let { eventChannel.send(it) }
                    },
                    onFailure = {
                        _uiState.update { current ->
                            current.copy(
                                editingSheetState =
                                    current.editingSheetState?.copy(isSaving = false),
                            )
                        }
                        eventChannel.send(
                            AnimeDetailsUiEvent.ShowSnackbar(
                                messageRes = CoreUiR.string.edit_list_status_update_error,
                                isError = true,
                            ),
                        )
                    },
                )
            }
        }

        fun onConfirmDeleteFromList() {
            val sheet = _uiState.value.editingSheetState ?: return
            if (sheet.isMutating) return
            _uiState.update { current ->
                current.copy(
                    editingSheetState =
                        current.editingSheetState?.copy(
                            isDeleting = true,
                            showDeleteConfirmation = false,
                        ),
                )
            }
            viewModelScope.launch {
                val result = statusInteractor.deleteAnime(sheet.animeId)
                result.fold(
                    onSuccess = {
                        _uiState.update { current ->
                            current.copy(
                                editingSheetState = null,
                                details =
                                    current.details?.copy(
                                        isInUserList = false,
                                        userStatus = null,
                                        userScore = null,
                                        numWatchedEpisodes = 0,
                                        isRewatching = false,
                                    ),
                            )
                        }
                    },
                    onFailure = {
                        _uiState.update { current ->
                            current.copy(
                                editingSheetState =
                                    current.editingSheetState?.copy(isDeleting = false),
                            )
                        }
                        eventChannel.send(
                            AnimeDetailsUiEvent.ShowSnackbar(
                                messageRes = CoreUiR.string.edit_list_status_delete_error,
                                isError = true,
                            ),
                        )
                    },
                )
            }
        }

        fun onMarkStatus(status: String) {
            viewModelScope.launch {
                statusInteractor
                    .updateStatus(
                        animeId,
                        UpdateUserAnimeStatusParams(status = status),
                    ).fold(
                        onSuccess = {
                            _uiState.update { current ->
                                current.copy(
                                    details = current.details?.copy(userStatus = status),
                                )
                            }
                        },
                        onFailure = {
                            eventChannel.send(
                                AnimeDetailsUiEvent.ShowSnackbar(
                                    messageRes = CoreUiR.string.edit_list_status_update_error,
                                    isError = true,
                                ),
                            )
                        },
                    )
            }
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

private fun applySavedSheetToDetails(
    current: AnimeDetails?,
    sheet: EditAnimeStatusSheetState,
): AnimeDetails? =
    current?.copy(
        isInUserList = true,
        userStatus = sheet.status,
        userScore = if (sheet.score > 0) sheet.score else null,
        numWatchedEpisodes = sheet.episodesWatched,
        isRewatching = sheet.isRewatching,
    )

private fun shouldPromptDetailsCompletion(sheet: EditAnimeStatusSheetState): Boolean {
    val total = sheet.totalEpisodes ?: return false
    return sheet.status != "completed" &&
        total > 0 &&
        sheet.episodesWatched >= total &&
        sheet.initialEpisodesWatched < total
}

private fun shouldPromptDetailsWatching(sheet: EditAnimeStatusSheetState): Boolean =
    sheet.initialStatus == "completed" &&
        sheet.status == "completed" &&
        sheet.episodesWatched < sheet.initialEpisodesWatched

private fun resolveDetailsPromptEvent(sheet: EditAnimeStatusSheetState): AnimeDetailsUiEvent? =
    when {
        shouldPromptDetailsWatching(sheet) ->
            AnimeDetailsUiEvent.ShowWatchingPrompt(sheet.animeId, sheet.title)
        shouldPromptDetailsCompletion(sheet) ->
            AnimeDetailsUiEvent.ShowCompletionPrompt(sheet.animeId, sheet.title)
        else -> null
    }
