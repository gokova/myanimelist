package com.gokova.myanimelist.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.ai.model.AiException
import com.gokova.myanimelist.core.datastore.AiPreferences
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.domain.usecase.AiSearchAnimeUseCase
import com.gokova.myanimelist.feature.search.domain.usecase.LoadMoreSearchResultsUseCase
import com.gokova.myanimelist.feature.search.domain.usecase.SearchAnimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class SearchViewModel
    @Inject
    constructor(
        private val searchAnimeUseCase: SearchAnimeUseCase,
        private val loadMoreSearchResultsUseCase: LoadMoreSearchResultsUseCase,
        private val aiSearchAnimeUseCase: AiSearchAnimeUseCase,
        private val aiPreferences: AiPreferences,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial())
        val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

        private val eventChannel = Channel<SearchUiEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()

        private var standardState: SearchUiState = SearchUiState.Initial()
        private var aiState: SearchUiState = SearchUiState.AiInitial()
        private var searchJob: Job? = null
        private var paginationJob: Job? = null
        private var nextPageUrl: String? = null

        fun onQueryChange(newQuery: String) {
            val current = _uiState.value
            if (newQuery != current.query) {
                searchJob?.cancel()
                paginationJob?.cancel()
            }
            val updated =
                when (current) {
                    is SearchUiState.Content ->
                        current.copy(
                            query = newQuery,
                            isLoadingMore = false,
                            loadMoreErrorRes = null,
                        )
                    else ->
                        SearchUiState.Initial(
                            query = newQuery,
                            aiPrompt = current.aiPrompt,
                        )
                }
            standardState = updated
            _uiState.value = updated
        }

        fun onSearch() {
            val currentQuery = _uiState.value.query.trim()
            if (currentQuery.length < SearchAnimeUseCase.MIN_QUERY_LENGTH) return

            searchJob?.cancel()
            paginationJob?.cancel()
            nextPageUrl = null
            _uiState.value =
                SearchUiState.Searching(
                    query = currentQuery,
                    aiPrompt = _uiState.value.aiPrompt,
                )

            searchJob =
                viewModelScope.launch {
                    searchAnimeUseCase(currentQuery)
                        .onSuccess { page ->
                            val latest = _uiState.value
                            if (latest.mode != SearchMode.STANDARD ||
                                latest.query != currentQuery
                            ) {
                                return@launch
                            }

                            nextPageUrl = page.nextUrl
                            val newState =
                                if (page.items.isEmpty()) {
                                    SearchUiState.Empty(
                                        query = currentQuery,
                                        aiPrompt = latest.aiPrompt,
                                    )
                                } else {
                                    SearchUiState.Content(
                                        query = currentQuery,
                                        aiPrompt = latest.aiPrompt,
                                        animes = page.items,
                                        hasMore = !page.nextUrl.isNullOrBlank(),
                                    )
                                }
                            standardState = newState
                            _uiState.value = newState
                        }.onFailure { throwable ->
                            val latest = _uiState.value
                            if (latest.mode != SearchMode.STANDARD ||
                                latest.query != currentQuery
                            ) {
                                return@launch
                            }

                            val errorState =
                                SearchUiState.Error(
                                    query = currentQuery,
                                    aiPrompt = latest.aiPrompt,
                                    message = throwable.localizedMessage,
                                )
                            standardState = errorState
                            _uiState.value = errorState
                        }
                }
        }

        fun onLoadMore() {
            val current = _uiState.value as? SearchUiState.Content ?: return
            val url = nextPageUrl
            if (!current.hasMore || current.isLoadingMore || url.isNullOrBlank()) return

            val requestQuery = current.query
            paginationJob?.cancel()
            _uiState.value = current.copy(isLoadingMore = true, loadMoreErrorRes = null)

            paginationJob =
                viewModelScope.launch {
                    loadMoreSearchResultsUseCase(url)
                        .onSuccess { nextPage ->
                            val latest = _uiState.value as? SearchUiState.Content ?: return@launch
                            if (latest.mode != SearchMode.STANDARD ||
                                latest.query != requestQuery
                            ) {
                                return@launch
                            }

                            nextPageUrl = nextPage.nextUrl
                            val existingIds = latest.animes.map { it.id }.toSet()
                            val uniqueNewItems = nextPage.items.filter { it.id !in existingIds }
                            val merged =
                                latest.copy(
                                    animes = latest.animes + uniqueNewItems,
                                    hasMore = !nextPage.nextUrl.isNullOrBlank(),
                                    isLoadingMore = false,
                                    loadMoreErrorRes = null,
                                )
                            standardState = merged
                            _uiState.value = merged
                        }.onFailure {
                            val latest = _uiState.value as? SearchUiState.Content ?: return@launch
                            if (latest.mode != SearchMode.STANDARD ||
                                latest.query != requestQuery
                            ) {
                                return@launch
                            }

                            val errorState =
                                latest.copy(
                                    isLoadingMore = false,
                                    loadMoreErrorRes = R.string.search_load_more_error,
                                )
                            standardState = errorState
                            _uiState.value = errorState
                            eventChannel.send(
                                SearchUiEvent.ShowSnackbar(R.string.search_load_more_error),
                            )
                        }
                }
        }

        fun onClearQuery() {
            searchJob?.cancel()
            paginationJob?.cancel()
            nextPageUrl = null
            val cleared = SearchUiState.Initial(query = "", aiPrompt = _uiState.value.aiPrompt)
            standardState = cleared
            _uiState.value = cleared
        }

        fun onRetrySearch() {
            onSearch()
        }

        fun onToggleMode() {
            searchJob?.cancel()
            paginationJob?.cancel()
            viewModelScope.launch {
                val current = _uiState.value
                if (current.mode == SearchMode.STANDARD) {
                    val key = aiPreferences.geminiApiKey.first()
                    if (key.isNullOrBlank()) {
                        setMissingKeyDialogVisible(true)
                        return@launch
                    }
                    if (aiState is SearchUiState.AiSearching) {
                        aiState =
                            SearchUiState.AiInitial(
                                query = aiState.query,
                                aiPrompt = aiState.aiPrompt,
                            )
                    }
                    _uiState.value = aiState
                } else {
                    if (standardState is SearchUiState.Searching) {
                        standardState =
                            SearchUiState.Initial(
                                query = standardState.query,
                                aiPrompt = standardState.aiPrompt,
                            )
                    }
                    _uiState.value = standardState
                }
            }
        }

        fun onAiPromptChange(newPrompt: String) {
            val updated =
                when (val current = _uiState.value) {
                    is SearchUiState.AiContent -> current.copy(aiPrompt = newPrompt)
                    else ->
                        SearchUiState.AiInitial(
                            query = current.query,
                            aiPrompt = newPrompt,
                        )
                }
            aiState = updated
            _uiState.value = updated
        }

        fun onAiSearch() {
            val prompt = _uiState.value.aiPrompt.trim()
            if (prompt.isBlank()) return

            searchJob?.cancel()
            _uiState.value =
                SearchUiState.AiSearching(
                    query = _uiState.value.query,
                    aiPrompt = prompt,
                )

            searchJob =
                viewModelScope.launch {
                    aiSearchAnimeUseCase(prompt)
                        .onSuccess { result ->
                            val latest = _uiState.value
                            if (latest.mode != SearchMode.AI ||
                                latest.aiPrompt.trim() != prompt
                            ) {
                                return@launch
                            }

                            val newState =
                                if (result.items.isEmpty()) {
                                    SearchUiState.AiEmpty(
                                        query = latest.query,
                                        aiPrompt = prompt,
                                        intentSummary = result.intentSummary,
                                        explanation = result.explanation,
                                    )
                                } else {
                                    SearchUiState.AiContent(
                                        query = latest.query,
                                        aiPrompt = prompt,
                                        result = result,
                                    )
                                }
                            aiState = newState
                            _uiState.value = newState
                        }.onFailure { throwable ->
                            val latest = _uiState.value
                            if (latest.mode != SearchMode.AI ||
                                latest.aiPrompt.trim() != prompt
                            ) {
                                return@launch
                            }

                            val errorState = mapAiError(throwable, latest.query, prompt)
                            aiState = errorState
                            _uiState.value = errorState
                        }
                }
        }

        fun setAiInfoDialogVisible(visible: Boolean) {
            val current = _uiState.value
            _uiState.value =
                when (current) {
                    is SearchUiState.AiContent -> current.copy(showAiInfoDialog = visible)
                    is SearchUiState.AiEmpty -> current.copy(showAiInfoDialog = visible)
                    is SearchUiState.AiError -> current.copy(showAiInfoDialog = visible)
                    else -> current
                }
        }

        fun setMissingKeyDialogVisible(visible: Boolean) {
            val current = _uiState.value
            _uiState.value =
                when (current) {
                    is SearchUiState.Initial -> current.copy(showMissingKeyDialog = visible)
                    is SearchUiState.Searching -> current.copy(showMissingKeyDialog = visible)
                    is SearchUiState.Content -> current.copy(showMissingKeyDialog = visible)
                    is SearchUiState.Empty -> current.copy(showMissingKeyDialog = visible)
                    is SearchUiState.Error -> current.copy(showMissingKeyDialog = visible)
                    is SearchUiState.AiInitial -> current.copy(showMissingKeyDialog = visible)
                    is SearchUiState.AiSearching -> current.copy(showMissingKeyDialog = visible)
                    is SearchUiState.AiContent -> current.copy(showMissingKeyDialog = visible)
                    is SearchUiState.AiEmpty -> current.copy(showMissingKeyDialog = visible)
                    is SearchUiState.AiError -> current.copy(showMissingKeyDialog = visible)
                }
        }

        fun onConfigureKeyClicked() {
            setMissingKeyDialogVisible(false)
            viewModelScope.launch {
                eventChannel.send(SearchUiEvent.NavigateToProfile)
            }
        }
    }

private fun mapAiError(
    throwable: Throwable,
    query: String,
    prompt: String,
): SearchUiState.AiError {
    val (errorRes, isMissing) =
        when (throwable) {
            is AiException.MissingApiKey -> R.string.search_error_message to true
            is AiException.QuotaExceeded -> R.string.ai_search_error_quota to false
            is AiException.SafetyBlocked -> R.string.ai_search_error_safety to false
            else -> R.string.ai_search_error_generic to false
        }

    return SearchUiState.AiError(
        query = query,
        aiPrompt = prompt,
        message = throwable.localizedMessage,
        messageRes = errorRes,
        isMissingKey = isMissing,
        showMissingKeyDialog = isMissing,
    )
}
