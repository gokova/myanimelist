package com.gokova.myanimelist.feature.search.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.search.presentation.SearchItemActions
import com.gokova.myanimelist.feature.search.presentation.SearchUiState

private val GRID_CELL_MIN_SIZE = 300.dp

@Composable
fun AiSearchContentResults(
    state: SearchUiState.AiContent,
    isExpanded: Boolean,
    itemActions: SearchItemActions,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isExpanded) {
        AiSearchGridResults(
            state = state,
            itemActions = itemActions,
            onShowInfo = onShowInfo,
            modifier = modifier,
        )
    } else {
        AiSearchListResults(
            state = state,
            itemActions = itemActions,
            onShowInfo = onShowInfo,
            modifier = modifier,
        )
    }
}

@Composable
private fun AiSearchListResults(
    state: SearchUiState.AiContent,
    itemActions: SearchItemActions,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = MaterialTheme.spacing.screenHorizontal,
                vertical = MaterialTheme.spacing.medium,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        item(key = "ai_header") {
            AiSearchResultHeader(
                intentSummary = state.result.intentSummary,
                resultCount = state.result.items.size,
                onShowInfo = onShowInfo,
            )
        }
        items(state.result.items, key = { it.id }) { anime ->
            SearchAnimeCard(
                anime = anime,
                onCardClick = itemActions.onAnimeClick,
                onPosterClick = { itemActions.onPosterClick(anime) },
            )
        }
    }
}

@Composable
private fun AiSearchGridResults(
    state: SearchUiState.AiContent,
    itemActions: SearchItemActions,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = MaterialTheme.spacing.screenHorizontal,
                vertical = MaterialTheme.spacing.medium,
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "ai_grid_header") {
            AiSearchResultHeader(
                intentSummary = state.result.intentSummary,
                resultCount = state.result.items.size,
                onShowInfo = onShowInfo,
            )
        }
        items(state.result.items, key = { it.id }) { anime ->
            SearchAnimeCard(
                anime = anime,
                onCardClick = itemActions.onAnimeClick,
                onPosterClick = { itemActions.onPosterClick(anime) },
            )
        }
    }
}
