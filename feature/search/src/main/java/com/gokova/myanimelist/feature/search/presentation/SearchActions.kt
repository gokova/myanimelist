package com.gokova.myanimelist.feature.search.presentation

data class SearchActions(
    val onQueryChange: (String) -> Unit,
    val onSearch: () -> Unit,
    val onClearQuery: () -> Unit,
    val onLoadMore: () -> Unit,
    val onRetrySearch: () -> Unit,
    val onBackClick: () -> Unit,
    val onAnimeClick: (Long) -> Unit,
)
