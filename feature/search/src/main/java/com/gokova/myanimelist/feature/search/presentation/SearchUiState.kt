package com.gokova.myanimelist.feature.search.presentation

import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem

sealed interface SearchUiState {
    val query: String

    data class Initial(
        override val query: String = "",
    ) : SearchUiState

    data class Searching(
        override val query: String,
    ) : SearchUiState

    data class Content(
        override val query: String,
        val animes: List<SearchAnimeItem>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val loadMoreError: String? = null,
    ) : SearchUiState

    data class Empty(
        override val query: String,
    ) : SearchUiState

    data class Error(
        override val query: String,
        val message: String,
    ) : SearchUiState
}
