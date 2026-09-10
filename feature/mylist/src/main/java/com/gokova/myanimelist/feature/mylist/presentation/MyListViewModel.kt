package com.gokova.myanimelist.feature.mylist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.feature.mylist.R
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SortOption
import com.gokova.myanimelist.feature.mylist.domain.model.SyncStatus
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MyListViewModel
    @Inject
    constructor(
        private val observeUserAnimeListUseCase: ObserveUserAnimeListUseCase,
        private val syncUserAnimeListUseCase: SyncUserAnimeListUseCase,
    ) : ViewModel() {
        private val selectedCategory = MutableStateFlow(ListFilterCategory.ALL)
        private val selectedSort = MutableStateFlow(SortOption.SCORE_DESC)
        private val isSyncing = MutableStateFlow(false)
        private val isRefreshing = MutableStateFlow(false)

        private val eventChannel = Channel<MyListUiEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()

        private val animeListFlow =
            combine(selectedCategory, selectedSort) { category, sort ->
                category to sort
            }.flatMapLatest { (category, sort) ->
                observeUserAnimeListUseCase(category, sort)
            }

        val uiState: StateFlow<MyListUiState> =
            combine(
                animeListFlow,
                selectedCategory,
                selectedSort,
                isSyncing,
                isRefreshing,
            ) { animeList, category, sort, syncing, refreshing ->
                MyListUiState(
                    animeList = animeList,
                    selectedCategory = category,
                    selectedSort = sort,
                    isSyncing = syncing,
                    isRefreshing = refreshing,
                    isLoadingInitial = false,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = MyListUiState(),
            )

        init {
            triggerSync(isPullToRefresh = false, force = false)
        }

        fun onCategorySelected(category: ListFilterCategory) {
            selectedCategory.value = category
        }

        fun onSortOptionSelected(sort: SortOption) {
            selectedSort.value = sort
        }

        fun onRefresh() {
            triggerSync(isPullToRefresh = true, force = true)
        }

        fun onRetrySync() {
            triggerSync(isPullToRefresh = false, force = true)
        }

        private fun triggerSync(
            isPullToRefresh: Boolean,
            force: Boolean,
        ) {
            if (isSyncing.value || isRefreshing.value) return

            viewModelScope.launch {
                try {
                    syncUserAnimeListUseCase(force = force).collect { status ->
                        when (status) {
                            SyncStatus.Started -> {
                                if (isPullToRefresh) {
                                    isRefreshing.value = true
                                } else {
                                    isSyncing.value = true
                                }
                                eventChannel.trySend(
                                    MyListUiEvent.ShowSnackbar(
                                        messageRes = R.string.my_list_syncing_message,
                                    ),
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
                } finally {
                    isSyncing.value = false
                    isRefreshing.value = false
                }
            }
        }

        companion object {
            private const val STOP_TIMEOUT_MILLIS = 5000L
        }
    }
