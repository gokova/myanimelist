package com.gokova.myanimelist.feature.mylist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.usecase.DeleteAnimeFromListUseCase
import com.gokova.myanimelist.core.domain.usecase.UpdateAnimeStatusUseCase
import com.gokova.myanimelist.core.ui.component.editlist.EditAnimeStatusSheetState
import com.gokova.myanimelist.feature.mylist.R
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SortOption
import com.gokova.myanimelist.feature.mylist.domain.model.SyncStatus
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.usecase.ObserveUserAnimeListUseCase
import com.gokova.myanimelist.feature.mylist.domain.usecase.SyncUserAnimeListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.gokova.myanimelist.core.ui.R as CoreUiR

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MyListViewModel
    @Inject
    constructor(
        private val observeUserAnimeListUseCase: ObserveUserAnimeListUseCase,
        private val syncUserAnimeListUseCase: SyncUserAnimeListUseCase,
        private val updateAnimeStatusUseCase: UpdateAnimeStatusUseCase,
        private val deleteAnimeFromListUseCase: DeleteAnimeFromListUseCase,
    ) : ViewModel() {
        private val selectedCategory = MutableStateFlow(ListFilterCategory.ALL)
        private val selectedSort = MutableStateFlow(SortOption.SCORE_DESC)
        private val isSyncing = MutableStateFlow(false)
        private val isRefreshing = MutableStateFlow(false)
        private val editingSheetState = MutableStateFlow<EditAnimeStatusSheetState?>(null)
        private val savingProgressAnimeIds = MutableStateFlow<Set<Long>>(emptySet())

        private val eventChannel = Channel<MyListUiEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()

        private val animeListFlow =
            combine(selectedCategory, selectedSort) { category, sort ->
                category to sort
            }.flatMapLatest { (category, sort) ->
                observeUserAnimeListUseCase(category, sort)
            }

        private val syncStateFlow =
            combine(isSyncing, isRefreshing) { syncing, refreshing -> syncing to refreshing }

        private val editStateFlow =
            combine(editingSheetState, savingProgressAnimeIds) { sheet, savingIds ->
                sheet to savingIds
            }

        val uiState: StateFlow<MyListUiState> =
            combine(
                animeListFlow,
                selectedCategory,
                selectedSort,
                syncStateFlow,
                editStateFlow,
            ) { animeList, category, sort, (syncing, refreshing), (sheet, savingIds) ->
                MyListUiState(
                    animeList = animeList,
                    selectedCategory = category,
                    selectedSort = sort,
                    isSyncing = syncing,
                    isRefreshing = refreshing,
                    isLoadingInitial = false,
                    editingSheetState = sheet,
                    savingProgressAnimeIds = savingIds,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = MyListUiState(),
            )

        init {
            onSync(isPullToRefresh = false, force = false)
        }

        fun onCategorySelected(category: ListFilterCategory) {
            selectedCategory.value = category
        }

        fun onSortOptionSelected(sort: SortOption) {
            selectedSort.value = sort
        }

        fun onSync(
            isPullToRefresh: Boolean = false,
            force: Boolean = true,
        ) {
            if (isSyncing.value || isRefreshing.value) return
            viewModelScope.launch {
                try {
                    syncUserAnimeListUseCase(force = force).collect { status ->
                        handleSyncStatus(status, isPullToRefresh)
                    }
                } finally {
                    isSyncing.value = false
                    isRefreshing.value = false
                }
            }
        }

        private suspend fun handleSyncStatus(
            status: SyncStatus,
            isPullToRefresh: Boolean,
        ) {
            when (status) {
                SyncStatus.Started -> {
                    if (isPullToRefresh) isRefreshing.value = true else isSyncing.value = true
                    eventChannel.trySend(
                        MyListUiEvent.ShowSnackbar(messageRes = R.string.my_list_syncing_message),
                    )
                }
                SyncStatus.Completed, SyncStatus.SkippedCooldown -> {
                    isSyncing.value = false
                    isRefreshing.value = false
                }
                is SyncStatus.Failure -> {
                    isSyncing.value = false
                    isRefreshing.value = false
                    eventChannel.send(
                        MyListUiEvent.ShowSnackbar(
                            messageRes = R.string.my_list_sync_failed_message,
                            actionRes = R.string.my_list_retry,
                            isError = true,
                        ),
                    )
                }
            }
        }

        fun onQuickIncrement(anime: UserAnime) {
            val isMutatingAnime =
                savingProgressAnimeIds.value.contains(anime.id) ||
                    editingSheetState.value?.animeId == anime.id
            val total = anime.totalEpisodes
            val isKnownTotal = total != null && total > 0
            val reachedTotal = isKnownTotal && anime.watchedEpisodes >= total
            if (isMutatingAnime || reachedTotal) return

            val nextEp = anime.watchedEpisodes + 1
            savingProgressAnimeIds.update { it + anime.id }
            viewModelScope.launch {
                val result =
                    updateAnimeStatusUseCase(
                        anime.id,
                        UpdateUserAnimeStatusParams(numWatchedEpisodes = nextEp),
                    )
                savingProgressAnimeIds.update { it - anime.id }
                result
                    .onSuccess {
                        if (isKnownTotal && nextEp >= total) {
                            eventChannel.send(
                                MyListUiEvent.ShowCompletionPrompt(anime.id, anime.displayTitle),
                            )
                        }
                    }.onFailure {
                        eventChannel.send(
                            MyListUiEvent.ShowSnackbar(
                                messageRes = CoreUiR.string.edit_list_status_update_error,
                                isError = true,
                            ),
                        )
                    }
            }
        }

        fun onMarkStatus(
            animeId: Long,
            status: String,
        ) {
            viewModelScope.launch {
                updateAnimeStatusUseCase(
                    animeId,
                    UpdateUserAnimeStatusParams(status = status),
                ).onFailure {
                    eventChannel.send(
                        MyListUiEvent.ShowSnackbar(
                            messageRes = CoreUiR.string.edit_list_status_update_error,
                            isError = true,
                        ),
                    )
                }
            }
        }

        fun setEditingAnime(anime: UserAnime?) {
            if (anime == null && editingSheetState.value?.isMutating == true) return
            if (anime != null && savingProgressAnimeIds.value.contains(anime.id)) return
            editingSheetState.value =
                anime?.let {
                    EditAnimeStatusSheetState(
                        animeId = it.id,
                        title = it.displayTitle,
                        status = it.userStatus.apiValue,
                        episodesWatched = it.watchedEpisodes,
                        totalEpisodes = it.totalEpisodes,
                        score = it.userScore,
                        isRewatching = it.isRewatching,
                        initialStatus = it.userStatus.apiValue,
                        initialEpisodesWatched = it.watchedEpisodes,
                    )
                }
        }

        fun updateEditingSheet(
            transform: (EditAnimeStatusSheetState) -> EditAnimeStatusSheetState,
        ) {
            editingSheetState.update { current ->
                if (current == null || current.isMutating) current else transform(current)
            }
        }

        fun onSaveEditSheet() {
            val sheet = editingSheetState.value ?: return
            if (sheet.isMutating || savingProgressAnimeIds.value.contains(sheet.animeId)) return
            editingSheetState.update { it?.copy(isSaving = true) }
            viewModelScope.launch {
                val params =
                    UpdateUserAnimeStatusParams(
                        status = sheet.status,
                        numWatchedEpisodes = sheet.episodesWatched,
                        score = sheet.score,
                        isRewatching = sheet.isRewatching,
                    )
                val result = updateAnimeStatusUseCase(sheet.animeId, params)
                result
                    .onSuccess {
                        editingSheetState.value = null
                        resolveMyListPromptEvent(sheet)?.let { eventChannel.send(it) }
                    }.onFailure {
                        editingSheetState.update { it?.copy(isSaving = false) }
                        eventChannel.send(
                            MyListUiEvent.ShowSnackbar(
                                messageRes = CoreUiR.string.edit_list_status_update_error,
                                isError = true,
                            ),
                        )
                    }
            }
        }

        fun onConfirmDeleteFromList() {
            val sheet = editingSheetState.value ?: return
            if (sheet.isMutating) return
            editingSheetState.update { it?.copy(isDeleting = true, showDeleteConfirmation = false) }
            viewModelScope.launch {
                val result = deleteAnimeFromListUseCase(sheet.animeId)
                result
                    .onSuccess {
                        editingSheetState.value = null
                    }.onFailure {
                        editingSheetState.update { it?.copy(isDeleting = false) }
                        eventChannel.send(
                            MyListUiEvent.ShowSnackbar(
                                messageRes = CoreUiR.string.edit_list_status_delete_error,
                                isError = true,
                            ),
                        )
                    }
            }
        }

        companion object {
            private const val STOP_TIMEOUT_MILLIS = 5000L
        }
    }

private fun shouldPromptMyListCompletion(sheet: EditAnimeStatusSheetState): Boolean {
    val total = sheet.totalEpisodes ?: return false
    return sheet.status != "completed" &&
        total > 0 &&
        sheet.episodesWatched >= total &&
        sheet.initialEpisodesWatched < total
}

private fun shouldPromptMyListWatching(sheet: EditAnimeStatusSheetState): Boolean =
    sheet.initialStatus == "completed" &&
        sheet.status == "completed" &&
        sheet.episodesWatched < sheet.initialEpisodesWatched

private fun resolveMyListPromptEvent(sheet: EditAnimeStatusSheetState): MyListUiEvent? =
    when {
        shouldPromptMyListWatching(sheet) ->
            MyListUiEvent.ShowWatchingPrompt(sheet.animeId, sheet.title)
        shouldPromptMyListCompletion(sheet) ->
            MyListUiEvent.ShowCompletionPrompt(sheet.animeId, sheet.title)
        else -> null
    }
