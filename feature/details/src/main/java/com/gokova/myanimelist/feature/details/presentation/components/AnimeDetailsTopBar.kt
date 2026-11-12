package com.gokova.myanimelist.feature.details.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.gokova.myanimelist.feature.details.R
import com.gokova.myanimelist.feature.details.presentation.AnimeDetailsActions
import com.gokova.myanimelist.feature.details.presentation.AnimeDetailsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailsTopBar(
    uiState: AnimeDetailsUiState,
    actions: AnimeDetailsActions,
    modifier: Modifier = Modifier,
) {
    val canToggleNotInterested = uiState.details?.let { !it.isInUserList } == true
    TopAppBar(
        title = {
            Text(
                text =
                    uiState.details?.displayTitle ?: stringResource(R.string.details_top_bar_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = actions.onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription =
                        stringResource(R.string.details_back_button_content_description),
                )
            }
        },
        actions = {
            if (uiState.hasFranchiseMap) {
                IconButton(onClick = actions.onFranchiseClick) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = stringResource(R.string.details_action_franchise_map),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (canToggleNotInterested) {
                DetailsNotInterestedAction(
                    isNotInterested = uiState.isNotInterested,
                    onToggle = actions.onToggleNotInterested,
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier = modifier,
    )
}

@Composable
private fun DetailsNotInterestedAction(
    isNotInterested: Boolean,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle) {
        val icon = if (isNotInterested) Icons.Filled.VisibilityOff else Icons.Outlined.VisibilityOff
        val labelRes =
            if (isNotInterested) {
                R.string.details_action_remove_not_interested
            } else {
                R.string.details_action_mark_not_interested
            }
        val tint =
            if (isNotInterested) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        Icon(
            imageVector = icon,
            contentDescription = stringResource(labelRes),
            tint = tint,
        )
    }
}
