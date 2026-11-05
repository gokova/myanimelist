package com.gokova.myanimelist.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.ai.model.AiException
import com.gokova.myanimelist.core.datastore.AiPreferences
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage
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
        private var activeRequestId: Long = 0L

        fun onQueryChange(newQuery: String) {
            val current = _uiState.value
            if (current is SearchUiState.Searching && newQuery != current.query) {
                searchJob?.cancel()
                paginationJob?.cancel()
            }
            val updated =
                when (current) {
                    is SearchUiState.Content ->
                        current.copy(
                            query = newQuery,
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
            val currentDraft = _uiState.value.query.trim()
            if (currentDraft.length < SearchAnimeUseCase.MIN_QUERY_LENGTH) return

            val requestId = ++activeRequestId
            searchJob?.cancel()
            paginationJob?.cancel()

            val current = _uiState.value
            if (current is SearchUiState.Content) {
                val replacingState =
                    current.copy(
                        query = currentDraft,
                        isReplacing = true,
                        replacementQuery = currentDraft,
                        isLoadingMore = false,
                        loadMoreErrorRes = null,
                    )
                standardState = replacingState
                _uiState.value = replacingState
            } else {
                nextPageUrl = null
                val searchingState =
                    SearchUiState.Searching(
                        query = currentDraft,
                        aiPrompt = current.aiPrompt,
                    )
                standardState = searchingState
                _uiState.value = searchingState
            }

            searchJob =
                viewModelScope.launch {
                    searchAnimeUseCase(currentDraft)
                        .onSuccess { page ->
                            handleSearchSuccess(page, requestId, currentDraft)
                        }.onFailure { throwable ->
                            handleSearchFailure(throwable, requestId, currentDraft)
                        }
                }
        }

        private suspend fun handleSearchSuccess(
            page: SearchResultPage,
            requestId: Long,
            submittedQuery: String,
        ) {
            if (requestId != activeRequestId || _uiState.value.mode != SearchMode.STANDARD) {
                return
            }

            nextPageUrl = page.nextUrl
            val currentUi = _uiState.value
            val newState =
                if (page.items.isEmpty()) {
                    SearchUiState.Empty(
                        query = currentUi.query,
                        aiPrompt = currentUi.aiPrompt,
                    )
                } else {
                    SearchUiState.Content(
                        query = currentUi.query,
                        resultQuery = submittedQuery,
                        aiPrompt = currentUi.aiPrompt,
                        animes = page.items,
                        hasMore = !page.nextUrl.isNullOrBlank(),
                    )
                }
            standardState = newState
            _uiState.value = newState
        }

        private suspend fun handleSearchFailure(
            throwable: Throwable,
            requestId: Long,
            submittedQuery: String,
        ) {
            if (requestId != activeRequestId || _uiState.value.mode != SearchMode.STANDARD) {
                return
            }

            val currentUi = _uiState.value
            if (currentUi is SearchUiState.Content && currentUi.isReplacing) {
                val reverted =
                    currentUi.copy(
                        isReplacing = false,
                        replacementQuery = null,
                    )
                standardState = reverted
                _uiState.value = reverted
                eventChannel.send(
                    SearchUiEvent.ShowSnackbar(
                        messageRes = R.string.search_replacement_failed,
                        formatArg = submittedQuery,
                    ),
                )
            } else {
                val errorState =
                    SearchUiState.Error(
                        query = currentUi.query,
                        aiPrompt = currentUi.aiPrompt,
                        message = throwable.localizedMessage,
                    )
                standardState = errorState
                _uiState.value = errorState
            }
        }

        fun onLoadMore() {
            val current = _uiState.value as? SearchUiState.Content ?: return
            val url = nextPageUrl
            if (canLoadMore(current, url) && url != null) {
                executeLoadMore(current, url, current.resultQuery, activeRequestId)
            }
        }

        private fun canLoadMore(
            current: SearchUiState.Content,
            url: String?,
        ): Boolean {
            val isBusy = current.isReplacing || current.isLoadingMore
            val hasTarget = current.hasMore && !url.isNullOrBlank()
            return !isBusy && hasTarget
        }

        private fun executeLoadMore(
            current: SearchUiState.Content,
            url: String,
            boundResultQuery: String,
            requestId: Long,
        ) {
            paginationJob?.cancel()
            val loadingMoreState = current.copy(isLoadingMore = true, loadMoreErrorRes = null)
            standardState = loadingMoreState
            _uiState.value = loadingMoreState

            paginationJob =
                viewModelScope.launch {
                    loadMoreSearchResultsUseCase(url)
                        .onSuccess { nextPage ->
                            handleLoadMoreSuccess(nextPage, boundResultQuery, requestId)
                        }.onFailure {
                            handleLoadMoreFailure(boundResultQuery, requestId)
                        }
                }
        }

        private fun handleLoadMoreSuccess(
            nextPage: SearchResultPage,
            boundResultQuery: String,
            requestId: Long,
        ) {
            val latest = _uiState.value as? SearchUiState.Content ?: return
            if (requestId != activeRequestId ||
                latest.mode != SearchMode.STANDARD ||
                latest.resultQuery != boundResultQuery
            ) {
                return
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
        }

        private suspend fun handleLoadMoreFailure(
            boundResultQuery: String,
            requestId: Long,
        ) {
            val latest = _uiState.value as? SearchUiState.Content ?: return
            if (requestId != activeRequestId ||
                latest.mode != SearchMode.STANDARD ||
                latest.resultQuery != boundResultQuery
            ) {
                return
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

        fun onClearQuery() {
            activeRequestId++
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
            activeRequestId++
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
