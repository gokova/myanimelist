package com.gokova.myanimelist.core.ui.component.editlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.R
import com.gokova.myanimelist.core.ui.theme.OnStatusCompletedContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusDroppedContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusOnHoldContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusPlanToWatchContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusWatchingContainer
import com.gokova.myanimelist.core.ui.theme.StatusCompletedContainer
import com.gokova.myanimelist.core.ui.theme.StatusDroppedContainer
import com.gokova.myanimelist.core.ui.theme.StatusOnHoldContainer
import com.gokova.myanimelist.core.ui.theme.StatusPlanToWatchContainer
import com.gokova.myanimelist.core.ui.theme.StatusWatchingContainer

private val statusOptions =
    listOf(
        "watching" to R.string.status_watching,
        "completed" to R.string.status_completed,
        "plan_to_watch" to R.string.status_plan_to_watch,
        "on_hold" to R.string.status_on_hold,
        "dropped" to R.string.status_dropped,
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusSelectorSection(
    currentStatus: String,
    onStatusSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.edit_list_status_section_status),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            statusOptions.forEach { (statusKey, stringRes) ->
                StatusOptionChip(
                    statusKey = statusKey,
                    label = stringResource(stringRes),
                    isSelected = currentStatus.equals(statusKey, ignoreCase = true),
                    enabled = enabled,
                    onClick = { onStatusSelected(statusKey) },
                )
            }
        }
    }
}

@Composable
private fun StatusOptionChip(
    statusKey: String,
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val (containerColor, contentColor) = getStatusColorPair(statusKey, isSelected)
    val borderColor = if (isSelected) containerColor else MaterialTheme.colorScheme.outlineVariant

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

private fun getStatusColorPair(
    statusKey: String,
    isSelected: Boolean,
): Pair<Color, Color> {
    if (!isSelected) {
        return Color.Transparent to Color.Unspecified
    }
    return when (statusKey.lowercase()) {
        "watching" -> StatusWatchingContainer to OnStatusWatchingContainer
        "completed" -> StatusCompletedContainer to OnStatusCompletedContainer
        "on_hold" -> StatusOnHoldContainer to OnStatusOnHoldContainer
        "dropped" -> StatusDroppedContainer to OnStatusDroppedContainer
        else -> StatusPlanToWatchContainer to OnStatusPlanToWatchContainer
    }
}

@Composable
fun EpisodeStepperSection(
    episodesWatched: Int,
    totalEpisodes: Int?,
    onEpisodesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isKnownTotal = totalEpisodes != null && totalEpisodes > 0
    val totalStr =
        if (isKnownTotal) {
            totalEpisodes.toString()
        } else {
            stringResource(R.string.edit_list_status_episodes_unknown)
        }
    val canDecrement = enabled && episodesWatched > 0
    val canIncrement = enabled && (!isKnownTotal || episodesWatched < totalEpisodes)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.edit_list_status_section_progress),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepperIconButton(
                icon = Icons.Default.Remove,
                contentDescription = stringResource(R.string.edit_list_status_decrement),
                enabled = canDecrement,
                onClick = { onEpisodesChange(episodesWatched - 1) },
            )

            EpisodeNumberInputField(
                episodesWatched = episodesWatched,
                totalEpisodes = totalEpisodes,
                onEpisodesChange = onEpisodesChange,
                enabled = enabled,
            )

            Text(
                text = stringResource(R.string.edit_list_status_episodes_total_format, totalStr),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            StepperIconButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.edit_list_status_increment),
                enabled = canIncrement,
                onClick = { onEpisodesChange(episodesWatched + 1) },
            )
        }
    }
}

@Composable
private fun StepperIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun RewatchingToggleSection(
    isRewatching: Boolean,
    onRewatchingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isRewatching,
            onCheckedChange = onRewatchingChange,
            enabled = enabled,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.edit_list_status_rewatching),
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
        )
    }
}

@Composable
fun RemoveFromListButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.edit_list_status_remove),
            fontWeight = FontWeight.Bold,
        )
    }
}
