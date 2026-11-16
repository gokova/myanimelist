package com.gokova.myanimelist.feature.mylist.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.mylist.R

@Composable
fun PendingSyncBanner(
    pendingCount: Int,
    conflictCount: Int,
    onSyncClick: () -> Unit,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pendingCount <= 0 && conflictCount <= 0) return

    val config = resolveBannerConfig(pendingCount, conflictCount, onSyncClick, onReviewClick)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = config.containerColor,
        contentColor = config.contentColor,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = config.onActionClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.medium,
                        vertical = MaterialTheme.spacing.small,
                    ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = config.contentColor,
                )
                Text(
                    text = config.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = config.contentColor,
                )
            }

            TextButton(
                onClick = config.onActionClick,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = config.contentColor,
                    ),
            ) {
                Text(
                    text = config.actionText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private data class PendingBannerConfig(
    val containerColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
    val message: String,
    val actionText: String,
    val onActionClick: () -> Unit,
)

@Composable
private fun resolveBannerConfig(
    pendingCount: Int,
    conflictCount: Int,
    onSyncClick: () -> Unit,
    onReviewClick: () -> Unit,
): PendingBannerConfig {
    val hasConflict = conflictCount > 0
    val containerColor =
        if (hasConflict) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    val contentColor =
        if (hasConflict) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }
    val icon = if (hasConflict) Icons.Default.Warning else Icons.Default.Schedule
    val message =
        if (hasConflict) {
            pluralStringResource(R.plurals.my_list_conflict_count, conflictCount, conflictCount)
        } else {
            pluralStringResource(R.plurals.my_list_pending_sync_count, pendingCount, pendingCount)
        }
    val actionText =
        if (hasConflict) {
            stringResource(R.string.my_list_review_conflicts)
        } else {
            stringResource(R.string.my_list_sync_pending_now)
        }
    val onActionClick = if (hasConflict) onReviewClick else onSyncClick

    return PendingBannerConfig(
        containerColor = containerColor,
        contentColor = contentColor,
        icon = icon,
        message = message,
        actionText = actionText,
        onActionClick = onActionClick,
    )
}

class PendingSyncBannerPreviewProvider : PreviewParameterProvider<Pair<Int, Int>> {
    override val values =
        sequenceOf(
            3 to 0,
            0 to 1,
            2 to 1,
        )

    override fun getDisplayName(index: Int): String =
        when (index) {
            0 -> "Pending Only"
            1 -> "Conflict Only"
            2 -> "Mixed"
            else -> "Item $index"
        }
}

@StandardPreviews
@Composable
private fun PendingSyncBannerPreview(
    @PreviewParameter(PendingSyncBannerPreviewProvider::class) counts: Pair<Int, Int>,
) {
    MyAnimeListTheme {
        Surface {
            PendingSyncBanner(
                pendingCount = counts.first,
                conflictCount = counts.second,
                onSyncClick = {},
                onReviewClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
