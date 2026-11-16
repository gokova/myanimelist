package com.gokova.myanimelist.core.ui.component.badge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.domain.model.SyncMutationState
import com.gokova.myanimelist.core.ui.R
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing

@Composable
fun SyncStatusBadge(
    state: SyncMutationState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val config = resolveBadgeColorsAndContent(state)
    val clickableModifier =
        if (onClick != null) {
            Modifier.clickable(role = Role.Button, onClick = onClick)
        } else {
            Modifier
        }
    val contentDesc = stringResource(config.descRes)

    Surface(
        modifier =
            modifier
                .then(clickableModifier)
                .semantics { this.contentDescription = contentDesc },
        shape = MaterialTheme.shapes.extraSmall,
        color = config.containerColor,
        contentColor = config.contentColor,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.extraSmall,
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state == SyncMutationState.SYNCING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = config.contentColor,
                )
            } else if (config.icon != null) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = config.contentColor,
                )
            }

            Text(
                text = stringResource(config.textRes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class BadgeConfig(
    val containerColor: Color,
    val contentColor: Color,
    val icon: ImageVector?,
    val textRes: Int,
    val descRes: Int,
)

@Composable
private fun resolveBadgeColorsAndContent(state: SyncMutationState): BadgeConfig =
    when (state) {
        SyncMutationState.PENDING ->
            BadgeConfig(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                icon = Icons.Default.Schedule,
                textRes = R.string.sync_status_pending,
                descRes = R.string.sync_status_pending_content_description,
            )
        SyncMutationState.SYNCING ->
            BadgeConfig(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = null,
                textRes = R.string.sync_status_syncing,
                descRes = R.string.sync_status_syncing_content_description,
            )
        SyncMutationState.CONFLICT ->
            BadgeConfig(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = Icons.Default.Warning,
                textRes = R.string.sync_status_conflict,
                descRes = R.string.sync_status_conflict_content_description,
            )
        SyncMutationState.FAILED ->
            BadgeConfig(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = Icons.Default.Warning,
                textRes = R.string.sync_status_failed,
                descRes = R.string.sync_status_failed_content_description,
            )
        SyncMutationState.AUTH_REQUIRED ->
            BadgeConfig(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = Icons.Default.Lock,
                textRes = R.string.sync_status_auth_required,
                descRes = R.string.sync_status_auth_required_content_description,
            )
    }

class SyncStatusBadgePreviewProvider : PreviewParameterProvider<SyncMutationState> {
    override val values =
        sequenceOf(
            SyncMutationState.PENDING,
            SyncMutationState.SYNCING,
            SyncMutationState.CONFLICT,
            SyncMutationState.FAILED,
            SyncMutationState.AUTH_REQUIRED,
        )

    override fun getDisplayName(index: Int): String =
        when (values.toList().getOrNull(index)) {
            SyncMutationState.PENDING -> "Pending"
            SyncMutationState.SYNCING -> "Syncing"
            SyncMutationState.CONFLICT -> "Conflict"
            SyncMutationState.FAILED -> "Failed"
            SyncMutationState.AUTH_REQUIRED -> "Auth Required"
            else -> "State $index"
        }
}

@StandardPreviews
@Composable
private fun SyncStatusBadgePreview(
    @PreviewParameter(SyncStatusBadgePreviewProvider::class) state: SyncMutationState,
) {
    MyAnimeListTheme {
        Surface {
            SyncStatusBadge(
                state = state,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
