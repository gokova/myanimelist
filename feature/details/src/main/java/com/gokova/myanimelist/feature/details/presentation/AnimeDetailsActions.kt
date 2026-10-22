package com.gokova.myanimelist.feature.details.presentation

import com.gokova.myanimelist.core.ui.component.editlist.EditAnimeStatusSheetActions

data class AnimeDetailsActions(
    val onBackClick: () -> Unit,
    val onRefresh: () -> Unit,
    val onRetry: () -> Unit,
    val onAddToList: () -> Unit,
    val onAnimeClick: (Long) -> Unit,
    val onEditStatusClick: () -> Unit = {},
    val onToggleNotInterested: () -> Unit = {},
    val onSheetActions: EditAnimeStatusSheetActions? = null,
)
