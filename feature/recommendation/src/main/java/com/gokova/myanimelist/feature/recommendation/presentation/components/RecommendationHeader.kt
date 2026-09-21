package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
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
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.recommendation.R
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType

@Composable
fun RecommendationHeader(
    count: Int,
    selectedType: RecommendationType,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
    sortConfig: HeaderSortConfig? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.large,
                    vertical = MaterialTheme.spacing.small,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val titleText =
            if (selectedType == RecommendationType.NEW_SEASONS) {
                pluralStringResource(R.plurals.new_seasons_count, count, count)
            } else {
                pluralStringResource(R.plurals.recommendations_count, count, count)
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
            if (selectedType == RecommendationType.NEW_SEASONS && sortConfig != null) {
                NewSeasonSortMenu(
                    selectedSort = sortConfig.selectedSort,
                    onSortSelected = sortConfig.onSortSelected,
                )
            }

            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.recommendation_btn_refresh),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
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
