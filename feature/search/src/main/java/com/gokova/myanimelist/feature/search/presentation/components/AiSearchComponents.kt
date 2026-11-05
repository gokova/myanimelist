package com.gokova.myanimelist.feature.search.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.presentation.SearchActions
import com.gokova.myanimelist.feature.search.presentation.SearchUiState

@Composable
fun AiSearchResultHeader(
    intentSummary: String,
    resultCount: Int,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AiHeaderTitleSection(intentSummary = intentSummary, modifier = Modifier.weight(1f))
            AiHeaderPicksSection(resultCount = resultCount, onShowInfo = onShowInfo)
        }
    }
}

@Composable
private fun AiHeaderTitleSection(
    intentSummary: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        Column {
            Text(
                text = stringResource(R.string.ai_search_header_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = intentSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AiHeaderPicksSection(
    resultCount: Int,
    onShowInfo: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$resultCount picks",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(
            onClick = onShowInfo,
            modifier = Modifier.size(MaterialTheme.spacing.minTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.ai_search_info_button_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AiSearchLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Text(
            text = stringResource(R.string.ai_search_loading_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            text = stringResource(R.string.ai_search_loading_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AiSearchEmptyState(
    isInitial: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Text(
            text =
                stringResource(
                    if (isInitial) {
                        R.string.ai_search_initial_title
                    } else {
                        R.string.ai_search_empty_title
                    },
                ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            text =
                stringResource(
                    if (isInitial) {
                        R.string.ai_search_initial_description
                    } else {
                        R.string.ai_search_empty_description
                    },
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AiSearchErrorState(
    state: SearchUiState.AiError,
    onRetry: () -> Unit,
    onConfigureKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.search_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            text = state.message ?: stringResource(state.messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        if (state.isMissingKey) {
            Button(onClick = onConfigureKey) {
                Text(stringResource(R.string.ai_search_missing_key_configure))
            }
        } else {
            Button(onClick = onRetry) {
                Text(stringResource(R.string.search_retry))
            }
        }
    }
}

@Composable
fun AiSearchDialogs(
    uiState: SearchUiState,
    actions: SearchActions,
) {
    if (uiState.showMissingKeyDialog) {
        AiMissingKeyDialog(actions = actions.aiActions.missingKeyActions)
    }

    if (uiState.showAiInfoDialog) {
        when (uiState) {
            is SearchUiState.AiContent -> {
                AiSearchInfoDialog(
                    intentSummary = uiState.result.intentSummary,
                    explanation = uiState.result.explanation,
                    unverifiedTitles = uiState.result.unverifiedCandidateTitles,
                    onDismiss = actions.aiActions.onDismissInfoDialog,
                )
            }
            is SearchUiState.AiEmpty -> {
                AiSearchInfoDialog(
                    intentSummary = uiState.intentSummary,
                    explanation = uiState.explanation,
                    unverifiedTitles = emptyList(),
                    onDismiss = actions.aiActions.onDismissInfoDialog,
                )
            }
            else -> Unit
        }
    }
}
