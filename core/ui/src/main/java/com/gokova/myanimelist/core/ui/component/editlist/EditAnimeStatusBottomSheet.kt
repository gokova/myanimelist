package com.gokova.myanimelist.core.ui.component.editlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAnimeStatusBottomSheet(
    state: EditAnimeStatusSheetState,
    actions: EditAnimeStatusSheetActions,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        SimulatedBottomSheetOverlay(state = state, actions = actions, modifier = modifier)
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                if (!state.isMutating) {
                    actions.onDismiss()
                }
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = modifier,
        ) {
            EditAnimeStatusSheetContent(
                state = state,
                actions = actions,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimulatedBottomSheetOverlay(
    state: EditAnimeStatusSheetState,
    actions: EditAnimeStatusSheetActions,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BottomSheetDefaults.DragHandle()
                EditAnimeStatusSheetContent(state = state, actions = actions)
            }
        }
    }
}

@Composable
fun EditAnimeStatusSheetContent(
    state: EditAnimeStatusSheetState,
    actions: EditAnimeStatusSheetActions,
    modifier: Modifier = Modifier,
) {
    val enabled = !state.isMutating
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.sheetHorizontal),
        ) {
            EditAnimeStatusHeader(
                title = state.title,
                isSaving = state.isSaving,
                onCloseClick = actions.onDismiss,
                onSaveClick = actions.onSave,
                isMutating = state.isMutating,
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        EditAnimeStatusFormBody(
            state = state,
            actions = actions,
            enabled = enabled,
            modifier = Modifier.weight(1f, fill = false),
        )
    }

    if (state.showDeleteConfirmation) {
        EditAnimeStatusDeleteDialog(
            animeTitle = state.title,
            onConfirm = actions.onConfirmDelete,
            onDismiss = actions.onDismissDeleteConfirmation,
        )
    }
}

@Composable
private fun EditAnimeStatusFormBody(
    state: EditAnimeStatusSheetState,
    actions: EditAnimeStatusSheetActions,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.sheetHorizontal)
                .verticalScroll(rememberScrollState()),
    ) {
        StatusSelectorSection(
            currentStatus = state.status,
            onStatusSelected = actions.onStatusChange,
            enabled = enabled,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        EpisodeStepperSection(
            episodesWatched = state.episodesWatched,
            totalEpisodes = state.totalEpisodes,
            onEpisodesChange = actions.onEpisodesChange,
            enabled = enabled,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        EditAnimeScoreSelector(
            score = state.score,
            onScoreChange = actions.onScoreChange,
            enabled = enabled,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        RewatchingToggleSection(
            isRewatching = state.isRewatching,
            onRewatchingChange = actions.onRewatchingChange,
            enabled = enabled,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        RemoveFromListButton(
            onClick = actions.onRequestDelete,
            enabled = enabled,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }
}

@StandardPreviews
@Composable
private fun EditAnimeStatusBottomSheetPreview() {
    MyAnimeListTheme {
        SimulatedBottomSheetOverlay(
            state =
                EditAnimeStatusSheetState(
                    animeId = 1L,
                    title = "Frieren: Beyond Journey's End",
                    status = "watching",
                    episodesWatched = 18,
                    totalEpisodes = 28,
                    score = 9,
                    isRewatching = false,
                ),
            actions =
                EditAnimeStatusSheetActions(
                    onStatusChange = {},
                    onEpisodesChange = {},
                    onScoreChange = {},
                    onRewatchingChange = {},
                    onSave = {},
                    onRequestDelete = {},
                    onConfirmDelete = {},
                    onDismissDeleteConfirmation = {},
                    onDismiss = {},
                ),
        )
    }
}
