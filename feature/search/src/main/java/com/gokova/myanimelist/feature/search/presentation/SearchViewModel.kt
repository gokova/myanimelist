package com.gokova.myanimelist.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage
import com.gokova.myanimelist.feature.search.domain.usecase.LoadMoreSearchResultsUseCase
import com.gokova.myanimelist.feature.search.domain.usecase.SearchAnimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val searchAnimeUseCase: SearchAnimeUseCase,
        private val loadMoreSearchResultsUseCase: LoadMoreSearchResultsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial())
        val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

        private val eventChannel = Channel<SearchUiEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()

        private var searchJob: Job? = null
        private var paginationJob: Job? = null
        private var nextPageUrl: String? = null

        fun onQueryChange(newQuery: String) {
            val current = _uiState.value
            if (newQuery != current.query) {
                searchJob?.cancel()
                paginationJob?.cancel()
            }
            _uiState.value =
                when (current) {
                    is SearchUiState.Initial -> current.copy(query = newQuery)
                    is SearchUiState.Searching -> SearchUiState.Initial(query = newQuery)
                    is SearchUiState.Content ->
                        current.copy(
                            query = newQuery,
                            isLoadingMore = false,
                            loadMoreError = null,
                        )
                    is SearchUiState.Empty -> SearchUiState.Initial(query = newQuery)
                    is SearchUiState.Error -> SearchUiState.Initial(query = newQuery)
                }
        }

        fun onSearch() {
            val currentQuery = _uiState.value.query.trim()
            if (currentQuery.length < SearchAnimeUseCase.MIN_QUERY_LENGTH) {
                return
            }

            searchJob?.cancel()
            paginationJob?.cancel()
            nextPageUrl = null
            _uiState.value = SearchUiState.Searching(query = currentQuery)

            searchJob =
                viewModelScope.launch {
                    searchAnimeUseCase(currentQuery)
                        .onSuccess { page ->
                            val latest = _uiState.value
                            if (latest.query != currentQuery) return@launch

                            nextPageUrl = page.nextUrl
                            _uiState.value =
                                if (page.items.isEmpty()) {
                                    SearchUiState.Empty(query = currentQuery)
                                } else {
                                    SearchUiState.Content(
                                        query = currentQuery,
                                        animes = page.items,
                                        hasMore = !page.nextUrl.isNullOrBlank(),
                                    )
                                }
                        }.onFailure { throwable ->
                            val latest = _uiState.value
                            if (latest.query != currentQuery) return@launch

                            _uiState.value =
                                SearchUiState.Error(
                                    query = currentQuery,
                                    message = throwable.localizedMessage ?: "Unknown error",
                                )
                        }
                }
        }

        fun onLoadMore() {
            val current = _uiState.value as? SearchUiState.Content
            val url = nextPageUrl
            if (current == null || url == null || current.isLoadingMore) {
                return
            }

            val requestQuery = current.query
            paginationJob?.cancel()
            _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)

            paginationJob =
                viewModelScope.launch {
                    loadMoreSearchResultsUseCase(url)
                        .onSuccess { page ->
                            handleLoadMoreSuccess(page, requestQuery)
                        }.onFailure { throwable ->
                            handleLoadMoreFailure(throwable, requestQuery)
                        }
                }
        }

        private fun handleLoadMoreSuccess(
            page: SearchResultPage,
            requestQuery: String,
        ) {
            val latest = _uiState.value as? SearchUiState.Content ?: return
            if (latest.query != requestQuery) return

            nextPageUrl = page.nextUrl
            val existingIds = latest.animes.map { it.id }.toSet()
            val uniqueNewItems = page.items.filter { it.id !in existingIds }

            _uiState.value =
                latest.copy(
                    animes = latest.animes + uniqueNewItems,
                    hasMore = !page.nextUrl.isNullOrBlank(),
                    isLoadingMore = false,
                    loadMoreError = null,
                )
        }

        private suspend fun handleLoadMoreFailure(
            throwable: Throwable,
            requestQuery: String,
        ) {
            val latest = _uiState.value as? SearchUiState.Content ?: return
            if (latest.query != requestQuery) return

            val errorMessage = throwable.localizedMessage ?: "Failed to load more"
            _uiState.value =
                latest.copy(
                    isLoadingMore = false,
                    loadMoreError = errorMessage,
                )
            eventChannel.send(SearchUiEvent.ShowSnackbar(R.string.search_load_more_error))
        }

        fun onClearQuery() {
            searchJob?.cancel()
            paginationJob?.cancel()
            nextPageUrl = null
            _uiState.value = SearchUiState.Initial(query = "")
        }

        fun onRetrySearch() {
            onSearch()
        }
    }
