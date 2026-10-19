package com.gokova.myanimelist.feature.mylist.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.core.ui.R as CoreUiR

@Composable
fun QuickIncrementButton(
    isSaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val quickAddDescription = stringResource(CoreUiR.string.edit_list_status_quick_add)
    val savingDescription = stringResource(CoreUiR.string.edit_list_status_quick_saving)
    val isInteractive = enabled && !isSaving

    Box(
        modifier =
            modifier
                .sizeIn(
                    minWidth = MaterialTheme.spacing.minTouchTarget,
                    minHeight = MaterialTheme.spacing.minTouchTarget,
                ).clickable(
                    enabled = isInteractive,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics {
                    contentDescription = if (isSaving) savingDescription else quickAddDescription
                },
        contentAlignment = Alignment.Center,
    ) {
        QuickIncrementBadge(
            isSaving = isSaving,
            enabled = enabled,
        )
    }
}

@Composable
private fun QuickIncrementBadge(
    isSaving: Boolean,
    enabled: Boolean,
) {
    val containerColor =
        if (enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        if (enabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.height(30.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = stringResource(CoreUiR.string.edit_list_status_quick_add),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
