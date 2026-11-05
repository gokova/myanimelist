package com.gokova.myanimelist.feature.search.presentation

import androidx.annotation.StringRes
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.domain.model.AiSearchResult
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem

enum class SearchMode {
    STANDARD,
    AI,
}

sealed interface SearchUiState {
    val mode: SearchMode
    val query: String
    val aiPrompt: String
    val showMissingKeyDialog: Boolean
    val showAiInfoDialog: Boolean

    data class Initial(
        override val query: String = "",
        override val aiPrompt: String = "",
        override val mode: SearchMode = SearchMode.STANDARD,
        override val showMissingKeyDialog: Boolean = false,
        override val showAiInfoDialog: Boolean = false,
    ) : SearchUiState

    data class Searching(
        override val query: String,
        override val aiPrompt: String = "",
        override val mode: SearchMode = SearchMode.STANDARD,
        override val showMissingKeyDialog: Boolean = false,
        override val showAiInfoDialog: Boolean = false,
    ) : SearchUiState

    data class Content(
        override val query: String,
        val animes: List<SearchAnimeItem>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        @StringRes val loadMoreErrorRes: Int? = null,
        override val aiPrompt: String = "",
        override val mode: SearchMode = SearchMode.STANDARD,
        override val showMissingKeyDialog: Boolean = false,
        override val showAiInfoDialog: Boolean = false,
    ) : SearchUiState

    data class Empty(
        override val query: String,
        override val aiPrompt: String = "",
        override val mode: SearchMode = SearchMode.STANDARD,
        override val showMissingKeyDialog: Boolean = false,
        override val showAiInfoDialog: Boolean = false,
    ) : SearchUiState

    data class Error(
        override val query: String,
        val message: String? = null,
        @StringRes val messageRes: Int = R.string.search_error_message,
        override val aiPrompt: String = "",
        override val mode: SearchMode = SearchMode.STANDARD,
        override val showMissingKeyDialog: Boolean = false,
        override val showAiInfoDialog: Boolean = false,
    ) : SearchUiState

    data class AiInitial(
        override val query: String = "",
        override val aiPrompt: String = "",
        override val mode: SearchMode = SearchMode.AI,
        override val showMissingKeyDialog: Boolean = false,
        override val showAiInfoDialog: Boolean = false,
    ) : SearchUiState

    data class AiSearching(
        override val query: String = "",
        override val aiPrompt: String,
        override val mode: SearchMode = SearchMode.AI,
        override val showMissingKeyDialog: Boolean = false,
        override val showAiInfoDialog: Boolean = false,
    ) : SearchUiState

    data class AiContent(
        override val query: String = "",
        override val aiPrompt: String,
        val result: AiSearchResult,
        override val mode: SearchMode = SearchMode.AI,
        override val showMissingKeyDialog: Boolean = false,
        override val showAiInfoDialog: Boolean = false,
    ) : SearchUiState

    data class AiEmpty(
        override val query: String = "",
        override val aiPrompt: String,
        val intentSummary: String,
        val explanation: String,
        override val mode: SearchMode = SearchMode.AI,
        override val showMissingKeyDialog: Boolean = false,
        override val showAiInfoDialog: Boolean = false,
    ) : SearchUiState

    data class AiError(
        override val query: String = "",
        override val aiPrompt: String,
        val message: String? = null,
        @StringRes val messageRes: Int = R.string.ai_search_error_generic,
        val isMissingKey: Boolean = false,
        override val mode: SearchMode = SearchMode.AI,
        override val showMissingKeyDialog: Boolean = false,
        override val showAiInfoDialog: Boolean = false,
    ) : SearchUiState
}
