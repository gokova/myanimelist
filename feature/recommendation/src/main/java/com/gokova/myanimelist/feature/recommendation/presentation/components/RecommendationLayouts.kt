package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime
import com.gokova.myanimelist.feature.recommendation.presentation.RecommendationUiState

private val GRID_CELL_MIN_SIZE = 300.dp

data class PosterPreviewData(
    val thumbnailUrl: String?,
    val largeImageUrl: String?,
    val title: String,
)

data class RecommendationItemActions(
    val onPosterClick: (PosterPreviewData) -> Unit,
    val onAnimeClick: (Long) -> Unit,
    val onNotInterested: (NotInterestedSheetData) -> Unit,
)

@Composable
fun RecommendationBodyContent(
    state: RecommendationUiState.Success,
    isExpanded: Boolean,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    if (state.selectedType == RecommendationType.NEW_SEASONS) {
        NewSeasonContent(
            newSeasons = state.newSeasons,
            isExpanded = isExpanded,
            itemActions = itemActions,
            modifier = modifier,
        )
    } else {
        RecommendationItemsContent(
            state = state,
            isExpanded = isExpanded,
            itemActions = itemActions,
            modifier = modifier,
        )
    }
}

@Composable
fun NewSeasonContent(
    newSeasons: List<NewSeasonAnime>,
    isExpanded: Boolean,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    val padding =
        PaddingValues(
            horizontal = MaterialTheme.spacing.screenHorizontal,
            vertical = MaterialTheme.spacing.small,
        )
    val spacing = MaterialTheme.spacing.medium
    val cardContent: @Composable (NewSeasonAnime) -> Unit = { anime ->
        val notInterestedAction = {
            itemActions.onNotInterested(
                NotInterestedSheetData(
                    animeId = anime.animeId,
                    title = anime.subtitleTitle ?: anime.displayTitle,
                    titleEnglish =
                        anime.displayTitle.takeIf { anime.subtitleTitle != null },
                    pictureUrl = anime.thumbnailUrl,
                ),
            )
        }
        NewSeasonCard(
            anime = anime,
            actions =
                RecommendationCardActions(
                    onPosterClick = {
                        itemActions.onPosterClick(
                            PosterPreviewData(
                                thumbnailUrl = anime.thumbnailUrl,
                                largeImageUrl = anime.largeImageUrl,
                                title = anime.displayTitle,
                            ),
                        )
                    },
                    onCardClick = { itemActions.onAnimeClick(anime.animeId) },
                    onCardLongClick = notInterestedAction,
                    onMoreClick = notInterestedAction,
                ),
        )
    }

    if (isExpanded) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
            modifier = modifier.fillMaxSize(),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(newSeasons, key = { it.animeId }) { cardContent(it) }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(newSeasons, key = { it.animeId }) { cardContent(it) }
        }
    }
}

@Composable
fun RecommendationItemsContent(
    state: RecommendationUiState.Success,
    isExpanded: Boolean,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    val padding =
        PaddingValues(
            horizontal = MaterialTheme.spacing.screenHorizontal,
            vertical = MaterialTheme.spacing.small,
        )
    val spacing = MaterialTheme.spacing.medium
    val cardContent: @Composable (RecommendedAnime) -> Unit = { anime ->
        val notInterestedAction = {
            itemActions.onNotInterested(
                NotInterestedSheetData(
                    animeId = anime.animeId,
                    title = anime.title,
                    titleEnglish = anime.titleEnglish,
                    pictureUrl = anime.thumbnailUrl,
                ),
            )
        }
        RecommendationCard(
            anime = anime,
            selectedType = state.selectedType,
            actions =
                RecommendationCardActions(
                    onPosterClick = {
                        itemActions.onPosterClick(
                            PosterPreviewData(
                                thumbnailUrl = anime.thumbnailUrl,
                                largeImageUrl = anime.largeImageUrl,
                                title = anime.displayTitle(),
                            ),
                        )
                    },
                    onCardClick = { itemActions.onAnimeClick(anime.animeId) },
                    onCardLongClick = notInterestedAction,
                    onMoreClick = notInterestedAction,
                ),
        )
    }

    if (isExpanded) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
            modifier = modifier.fillMaxSize(),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(state.recommendations, key = { it.animeId }) { cardContent(it) }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(state.recommendations, key = { it.animeId }) { cardContent(it) }
        }
    }
}
