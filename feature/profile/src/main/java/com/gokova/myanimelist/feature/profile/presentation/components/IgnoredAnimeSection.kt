package com.gokova.myanimelist.feature.profile.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.profile.R

private val BUTTON_MIN_HEIGHT = 48.dp
private val ICON_SIZE = 18.dp

@Composable
fun IgnoredAnimeSection(
    ignoredCount: Int,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.profile_ignored_anime_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.medium),
            shape = MaterialTheme.shapes.medium,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.medium),
            ) {
                IgnoredAnimeCountHeader(ignoredCount = ignoredCount)
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                IgnoredAnimeActionButtons(
                    ignoredCount = ignoredCount,
                    onExportClick = onExportClick,
                    onImportClick = onImportClick,
                )
            }
        }
    }
}

@Composable
private fun IgnoredAnimeCountHeader(
    ignoredCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.VisibilityOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(ICON_SIZE),
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        Text(
            text = stringResource(R.string.profile_ignored_anime_count_format, ignoredCount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun IgnoredAnimeActionButtons(
    ignoredCount: Int,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        OutlinedButton(
            onClick = onExportClick,
            enabled = ignoredCount > 0,
            modifier =
                Modifier
                    .weight(1f)
                    .height(BUTTON_MIN_HEIGHT),
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(ICON_SIZE),
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(R.string.profile_ignored_anime_export),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        FilledTonalButton(
            onClick = onImportClick,
            modifier =
                Modifier
                    .weight(1f)
                    .height(BUTTON_MIN_HEIGHT),
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                modifier = Modifier.size(ICON_SIZE),
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(R.string.profile_ignored_anime_import),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@StandardPreviews
@Composable
private fun IgnoredAnimeSectionPreview() {
    MyAnimeListTheme {
        Surface {
            IgnoredAnimeSection(
                ignoredCount = 12,
                onExportClick = {},
                onImportClick = {},
            )
        }
    }
}
