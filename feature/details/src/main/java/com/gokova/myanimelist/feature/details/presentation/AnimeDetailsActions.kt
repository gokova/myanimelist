package com.gokova.myanimelist.feature.details.presentation

data class AnimeDetailsActions(
    val onBackClick: () -> Unit,
    val onRefresh: () -> Unit,
    val onRetry: () -> Unit,
    val onAddToList: () -> Unit,
    val onAnimeClick: (Long) -> Unit,
)
