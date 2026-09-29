package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.recommendation.R

@Composable
fun BackgroundSyncDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        BackgroundSyncDialogPreviewOverlay(
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(R.string.background_sync_dialog_title))
            },
            text = {
                Text(text = stringResource(R.string.background_sync_dialog_message))
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text(text = stringResource(R.string.background_sync_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.background_sync_dialog_dismiss))
                }
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun BackgroundSyncDialogPreviewOverlay(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.padding(MaterialTheme.spacing.large),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.background_sync_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.background_sync_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.background_sync_dialog_dismiss))
                    }
                    Button(onClick = onConfirm) {
                        Text(text = stringResource(R.string.background_sync_dialog_confirm))
                    }
                }
            }
        }
    }
}

@StandardPreviews
@Composable
private fun BackgroundSyncDialogPreview() {
    MyAnimeListTheme {
        BackgroundSyncDialog(onConfirm = {}, onDismiss = {})
    }
}
