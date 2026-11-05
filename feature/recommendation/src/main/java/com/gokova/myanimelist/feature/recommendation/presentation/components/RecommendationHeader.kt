package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.recommendation.R
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationLabConfig
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType

@Composable
fun RecommendationHeader(
    config: RecommendationHeaderConfig,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
    labConfig: RecommendationLabConfig? = null,
    onTuneClick: () -> Unit = {},
) {
    val verticalPadding =
        if (config.compact) MaterialTheme.spacing.extraSmall else MaterialTheme.spacing.small
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.large,
                    vertical = verticalPadding,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val titleText =
            if (config.selectedType == RecommendationType.NEW_SEASONS) {
                pluralStringResource(R.plurals.new_seasons_count, config.count, config.count)
            } else {
                pluralStringResource(R.plurals.recommendations_count, config.count, config.count)
            }

        Text(
            text = titleText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            RecommendationRefreshStatus(
                isRefreshing = config.isRefreshing,
                isQueued = config.isQueued,
            )

            if (config.selectedType == RecommendationType.NEW_SEASONS &&
                config.sortConfig != null
            ) {
                NewSeasonSortMenu(
                    selectedSort = config.sortConfig.selectedSort,
                    onSortSelected = config.sortConfig.onSortSelected,
                )
            } else if (config.selectedType != RecommendationType.NEW_SEASONS) {
                RecommendationTuneButton(
                    isCustomized = labConfig != null && !labConfig.isDefault,
                    onTuneClick = onTuneClick,
                )
            }

            RecommendationRefreshButton(
                isBusy = config.isRefreshing || config.isQueued,
                onRefreshClick = onRefreshClick,
            )
        }
    }
}

@Composable
private fun RecommendationRefreshStatus(
    isRefreshing: Boolean,
    isQueued: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isRefreshing) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.recommendation_refreshing),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    } else if (isQueued) {
        Text(
            text = stringResource(R.string.recommendation_queued),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

@Composable
private fun RecommendationTuneButton(
    isCustomized: Boolean,
    onTuneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onTuneClick,
        modifier = modifier,
    ) {
        if (isCustomized) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(6.dp),
                    )
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription =
                        stringResource(R.string.recommendation_lab_tune_description),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription =
                    stringResource(R.string.recommendation_lab_tune_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecommendationRefreshButton(
    isBusy: Boolean,
    onRefreshClick: () -> Unit,
) {
    IconButton(
        onClick = onRefreshClick,
        enabled = !isBusy,
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.recommendation_btn_refresh),
            tint =
                if (isBusy) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
        )
    }
}

@Composable
private fun NewSeasonSortMenu(
    selectedSort: NewSeasonSortOption,
    onSortSelected: (NewSeasonSortOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.new_seasons_sort_menu_desc),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            NewSeasonSortOption.entries.forEach { sort ->
                val labelRes =
                    when (sort) {
                        NewSeasonSortOption.RELEASE_DATE_DESC ->
                            R.string.new_seasons_sort_release_date
                        NewSeasonSortOption.SCORE_DESC ->
                            R.string.new_seasons_sort_score
                        NewSeasonSortOption.TITLE_ASC ->
                            R.string.new_seasons_sort_title
                    }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight =
                                if (selectedSort == sort) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                        )
                    },
                    trailingIcon = {
                        if (selectedSort == sort) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    onClick = {
                        onSortSelected(sort)
                        expanded = false
                    },
                )
            }
        }
    }
}
