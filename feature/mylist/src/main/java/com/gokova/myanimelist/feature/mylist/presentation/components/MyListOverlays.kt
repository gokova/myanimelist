package com.gokova.myanimelist.feature.mylist.presentation.components

import androidx.compose.runtime.Composable
import com.gokova.myanimelist.core.ui.component.AnimePosterPreviewDialog
import com.gokova.myanimelist.core.ui.component.conflict.ConflictResolutionBottomSheet
import com.gokova.myanimelist.core.ui.component.editlist.EditAnimeStatusBottomSheet
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.presentation.MyListActions
import com.gokova.myanimelist.feature.mylist.presentation.MyListUiState

@Composable
fun MyListOverlays(
    uiState: MyListUiState,
    actions: MyListActions,
    previewAnime: UserAnime?,
    onDismissPreview: () -> Unit,
) {
    previewAnime?.let { anime ->
        AnimePosterPreviewDialog(
            thumbnailUrl = anime.thumbnailUrl,
            largeImageUrl = anime.largeImageUrl,
            title = anime.displayTitle,
            onDismiss = onDismissPreview,
        )
    }

    if (uiState.editingSheetState != null && actions.onSheetActions != null) {
        EditAnimeStatusBottomSheet(
            state = uiState.editingSheetState,
            actions = actions.onSheetActions,
        )
    }

    if (uiState.conflictSheetState != null && actions.onConflictSheetActions != null) {
        ConflictResolutionBottomSheet(
            state = uiState.conflictSheetState,
            actions = actions.onConflictSheetActions,
        )
    }
}
