package com.gokova.myanimelist.core.ui.component.conflict

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.domain.model.AnimeConflictValues
import com.gokova.myanimelist.core.ui.R
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing

data class ConflictSheetState(
    val animeId: Long,
    val animeTitle: String,
    val baseline: AnimeConflictValues,
    val device: AnimeConflictValues,
    val remote: AnimeConflictValues,
    val isResolving: Boolean = false,
)

data class ConflictSheetActions(
    val onKeepDevice: () -> Unit = {},
    val onUseRemote: () -> Unit = {},
    val onCustomEdit: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictResolutionBottomSheet(
    state: ConflictSheetState,
    actions: ConflictSheetActions,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        SimulatedConflictOverlay(state = state, actions = actions, modifier = modifier)
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                if (!state.isResolving) {
                    actions.onDismiss()
                }
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = modifier,
        ) {
            ConflictSheetContent(
                state = state,
                actions = actions,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimulatedConflictOverlay(
    state: ConflictSheetState,
    actions: ConflictSheetActions,
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
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                ConflictSheetContent(state = state, actions = actions)
            }
        }
    }
}

@Composable
internal fun ConflictSheetContent(
    state: ConflictSheetState,
    actions: ConflictSheetActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = MaterialTheme.spacing.large,
                    vertical = MaterialTheme.spacing.medium,
                ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        ConflictSheetHeader(title = state.animeTitle, onDismiss = actions.onDismiss)

        Text(
            text = stringResource(R.string.conflict_sheet_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ConflictComparisonTable(
            baseline = state.baseline,
            device = state.device,
            remote = state.remote,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        ConflictSheetButtons(
            state = state,
            actions = actions,
        )
    }
}

@Composable
private fun ConflictSheetHeader(
    title: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.conflict_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(MaterialTheme.spacing.minTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.conflict_sheet_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConflictComparisonTable(
    baseline: AnimeConflictValues,
    device: AnimeConflictValues,
    remote: AnimeConflictValues,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            ComparisonRow(
                label = stringResource(R.string.conflict_sheet_label_last_synced),
                episodes = baseline.episodesWatched,
                status = baseline.status,
                score = baseline.score,
                isHighlight = false,
            )
            ComparisonRow(
                label = stringResource(R.string.conflict_sheet_label_device),
                episodes = device.episodesWatched,
                status = device.status,
                score = device.score,
                isHighlight = true,
            )
            ComparisonRow(
                label = stringResource(R.string.conflict_sheet_label_remote),
                episodes = remote.episodesWatched,
                status = remote.status,
                score = remote.score,
                isHighlight = true,
            )
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    episodes: Int?,
    status: String?,
    score: Int?,
    isHighlight: Boolean,
) {
    val epText =
        if (episodes != null && episodes > 0) {
            stringResource(R.string.conflict_sheet_episodes, episodes)
        } else {
            stringResource(R.string.conflict_sheet_episodes_unwatched)
        }
    val scoreText =
        if (score != null && score > 0) {
            stringResource(R.string.conflict_sheet_score_format, score)
        } else {
            stringResource(R.string.conflict_sheet_score_unrated)
        }
    val statusText = status?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color =
                if (isHighlight) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Text(
            text = "$epText • $statusText • $scoreText",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Normal,
            color =
                if (isHighlight) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun ConflictSheetButtons(
    state: ConflictSheetState,
    actions: ConflictSheetActions,
) {
    val deviceEp = state.device.episodesWatched ?: 0
    val remoteEp = state.remote.episodesWatched ?: 0
    val deviceEpLabel = stringResource(R.string.conflict_sheet_episode_short, deviceEp)
    val remoteEpLabel = stringResource(R.string.conflict_sheet_episode_short, remoteEp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Button(
            onClick = actions.onKeepDevice,
            enabled = !state.isResolving,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            if (state.isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.conflict_sheet_action_keep_device, deviceEpLabel))
            }
        }

        FilledTonalButton(
            onClick = actions.onUseRemote,
            enabled = !state.isResolving,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(stringResource(R.string.conflict_sheet_action_use_remote, remoteEpLabel))
        }

        OutlinedButton(
            onClick = actions.onCustomEdit,
            enabled = !state.isResolving,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(stringResource(R.string.conflict_sheet_action_edit))
        }
    }
}

class ConflictSheetStatePreviewProvider : PreviewParameterProvider<ConflictSheetState> {
    override val values =
        sequenceOf(
            ConflictSheetState(
                animeId = 1L,
                animeTitle = "Sousou no Frieren",
                baseline =
                    AnimeConflictValues(
                        episodesWatched = 5,
                        status = "watching",
                        score = 9,
                    ),
                device =
                    AnimeConflictValues(
                        episodesWatched = 7,
                        status = "watching",
                        score = 9,
                    ),
                remote =
                    AnimeConflictValues(
                        episodesWatched = 9,
                        status = "watching",
                        score = 9,
                    ),
            ),
        )

    override fun getDisplayName(index: Int): String = "Conflict State"
}

@StandardPreviews
@Composable
private fun ConflictResolutionBottomSheetPreview(
    @PreviewParameter(ConflictSheetStatePreviewProvider::class) state: ConflictSheetState,
) {
    MyAnimeListTheme {
        ConflictResolutionBottomSheet(
            state = state,
            actions = ConflictSheetActions(),
        )
    }
}
