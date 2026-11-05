package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.MotionTokens
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
    key(state.selectedType) {
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
}

@Composable
fun NewSeasonContent(
    newSeasons: List<NewSeasonAnime>,
    isExpanded: Boolean,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    if (isExpanded) {
        NewSeasonGrid(newSeasons = newSeasons, itemActions = itemActions, modifier = modifier)
    } else {
        NewSeasonList(newSeasons = newSeasons, itemActions = itemActions, modifier = modifier)
    }
}

@Composable
private fun NewSeasonGrid(
    newSeasons: List<NewSeasonAnime>,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    val padding =
        PaddingValues(
            horizontal = MaterialTheme.spacing.screenHorizontal,
            vertical = MaterialTheme.spacing.small,
        )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
        modifier = modifier.fillMaxSize(),
        contentPadding = padding,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(newSeasons, key = { it.animeId }) { anime ->
            NewSeasonItemCard(
                anime = anime,
                itemActions = itemActions,
                modifier =
                    Modifier.animateItem(
                        fadeInSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingEnter,
                            ),
                        fadeOutSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingExit,
                            ),
                        placementSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingStandard,
                            ),
                    ),
            )
        }
    }
}

@Composable
private fun NewSeasonList(
    newSeasons: List<NewSeasonAnime>,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    val padding =
        PaddingValues(
            horizontal = MaterialTheme.spacing.screenHorizontal,
            vertical = MaterialTheme.spacing.small,
        )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(newSeasons, key = { it.animeId }) { anime ->
            NewSeasonItemCard(
                anime = anime,
                itemActions = itemActions,
                modifier =
                    Modifier.animateItem(
                        fadeInSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingEnter,
                            ),
                        fadeOutSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingExit,
                            ),
                        placementSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingStandard,
                            ),
                    ),
            )
        }
    }
}

@Composable
private fun NewSeasonItemCard(
    anime: NewSeasonAnime,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    val notInterestedAction = {
        itemActions.onNotInterested(
            NotInterestedSheetData(
                animeId = anime.animeId,
                title = anime.subtitleTitle ?: anime.displayTitle,
                titleEnglish = anime.displayTitle.takeIf { anime.subtitleTitle != null },
                pictureUrl = anime.thumbnailUrl,
            ),
        )
    }
    NewSeasonCard(
        anime = anime,
        modifier = modifier,
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

@Composable
fun RecommendationItemsContent(
    state: RecommendationUiState.Success,
    isExpanded: Boolean,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    if (isExpanded) {
        RecommendationGrid(state = state, itemActions = itemActions, modifier = modifier)
    } else {
        RecommendationList(state = state, itemActions = itemActions, modifier = modifier)
    }
}

@Composable
private fun RecommendationGrid(
    state: RecommendationUiState.Success,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    val padding =
        PaddingValues(
            horizontal = MaterialTheme.spacing.screenHorizontal,
            vertical = MaterialTheme.spacing.small,
        )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
        modifier = modifier.fillMaxSize(),
        contentPadding = padding,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(state.recommendations, key = { it.animeId }) { anime ->
            RecommendationItemCard(
                anime = anime,
                selectedType = state.selectedType,
                itemActions = itemActions,
                modifier =
                    Modifier.animateItem(
                        fadeInSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingEnter,
                            ),
                        fadeOutSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingExit,
                            ),
                        placementSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingStandard,
                            ),
                    ),
            )
        }
    }
}

@Composable
private fun RecommendationList(
    state: RecommendationUiState.Success,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    val padding =
        PaddingValues(
            horizontal = MaterialTheme.spacing.screenHorizontal,
            vertical = MaterialTheme.spacing.small,
        )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(state.recommendations, key = { it.animeId }) { anime ->
            RecommendationItemCard(
                anime = anime,
                selectedType = state.selectedType,
                itemActions = itemActions,
                modifier =
                    Modifier.animateItem(
                        fadeInSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingEnter,
                            ),
                        fadeOutSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingExit,
                            ),
                        placementSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_ITEM_PLACEMENT,
                                easing = MotionTokens.EasingStandard,
                            ),
                    ),
            )
        }
    }
}

@Composable
private fun RecommendationItemCard(
    anime: RecommendedAnime,
    selectedType: RecommendationType,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
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
        selectedType = selectedType,
        modifier = modifier,
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
