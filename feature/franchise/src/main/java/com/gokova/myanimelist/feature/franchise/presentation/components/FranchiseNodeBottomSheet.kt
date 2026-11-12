package com.gokova.myanimelist.feature.franchise.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.franchise.R
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNode
import com.gokova.myanimelist.feature.franchise.presentation.FranchiseActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FranchiseNodeBottomSheet(
    node: FranchiseNode,
    isUpdatingEpisode: Boolean,
    actions: FranchiseActions,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = actions.onDismissBottomSheet,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.medium)
                    .padding(bottom = MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            FranchiseBottomSheetHeader(node = node)

            FranchiseBottomSheetQuickActions(
                node = node,
                isUpdatingEpisode = isUpdatingEpisode,
                onIncrementEpisode = actions.onIncrementEpisode,
                onUnignoreAnime = actions.onUnignoreAnime,
            )

            Button(
                onClick = {
                    actions.onDismissBottomSheet()
                    actions.onOpenDetails(node.animeId)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.franchise_sheet_open_details))
            }
        }
    }
}

@Composable
private fun FranchiseBottomSheetHeader(
    node: FranchiseNode,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = node.posterMedium,
            contentDescription = null,
            modifier =
                Modifier
                    .width(64.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = node.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = node.formattedMediaType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            if (node.isInUserList && node.userStatus != null) {
                val episodesTotal = node.numEpisodes?.toString() ?: "?"
                Text(
                    text =
                        stringResource(
                            R.string.franchise_sheet_progress,
                            node.episodesWatched,
                            episodesTotal,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FranchiseBottomSheetQuickActions(
    node: FranchiseNode,
    isUpdatingEpisode: Boolean,
    onIncrementEpisode: (Long) -> Unit,
    onUnignoreAnime: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (node.userStatus != "watching" && !node.isNotInterested) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        if (node.userStatus == "watching") {
            OutlinedButton(
                onClick = { onIncrementEpisode(node.animeId) },
                enabled = !isUpdatingEpisode,
                modifier = Modifier.weight(1f),
            ) {
                if (isUpdatingEpisode) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.franchise_sheet_increment_episode))
                }
            }
        }

        if (node.isNotInterested) {
            OutlinedButton(
                onClick = { onUnignoreAnime(node.animeId) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.franchise_sheet_unignore))
            }
        }
    }
}
