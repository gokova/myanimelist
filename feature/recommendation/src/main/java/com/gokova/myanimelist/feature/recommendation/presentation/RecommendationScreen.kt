package com.gokova.myanimelist.feature.recommendation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.recommendation.R
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime
import com.gokova.myanimelist.feature.recommendation.presentation.components.RecommendationCard
import com.gokova.myanimelist.feature.recommendation.presentation.components.RecommendationEmptyState
import com.gokova.myanimelist.feature.recommendation.presentation.components.RecommendationHeader
import com.gokova.myanimelist.feature.recommendation.presentation.components.RecommendationPillSelector

private val EXPANDED_WIDTH_BREAKPOINT = 600.dp
private val GRID_CELL_MIN_SIZE = 300.dp

@Composable
fun RecommendationScreen(
    modifier: Modifier = Modifier,
    viewModel: RecommendationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecommendationContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

@Composable
fun RecommendationContent(
    uiState: RecommendationUiState,
    onEvent: (RecommendationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            RecommendationUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            RecommendationUiState.EmptyInsufficientData -> {
                RecommendationEmptyState(
                    titleRes = R.string.recommendation_empty_insufficient_title,
                    subtitleRes = R.string.recommendation_empty_insufficient_subtitle,
                )
            }
            RecommendationUiState.Calculating -> {
                RecommendationEmptyState(
                    titleRes = R.string.recommendation_calculating_title,
                    subtitleRes = R.string.recommendation_calculating_subtitle,
                    actionButtonTextRes = R.string.recommendation_btn_calculate_now,
                    onActionClick = { onEvent(RecommendationUiEvent.CalculateNow) },
                )
            }
            is RecommendationUiState.Error -> {
                RecommendationEmptyState(
                    titleRes = R.string.recommendation_calculating_title,
                    subtitleRes = R.string.recommendation_calculating_subtitle,
                    actionButtonTextRes = R.string.recommendation_btn_calculate_now,
                    onActionClick = { onEvent(RecommendationUiEvent.CalculateNow) },
                )
            }
            is RecommendationUiState.Success -> {
                RecommendationSuccessContent(
                    state = uiState,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun RecommendationSuccessContent(
    state: RecommendationUiState.Success,
    onEvent: (RecommendationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val isExpanded = screenWidthDp >= EXPANDED_WIDTH_BREAKPOINT

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.screenHorizontal,
                        vertical = MaterialTheme.spacing.small,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            RecommendationPillSelector(
                selectedType = state.selectedType,
                onTypeSelected = { onEvent(RecommendationUiEvent.SelectType(it)) },
            )
        }

        RecommendationHeader(
            count = state.recommendations.size,
            onRefreshClick = { onEvent(RecommendationUiEvent.Refresh) },
        )

        if (isExpanded) {
            RecommendationGrid(
                recommendations = state.recommendations,
                selectedType = state.selectedType,
                modifier = Modifier.weight(1f),
            )
        } else {
            RecommendationList(
                recommendations = state.recommendations,
                selectedType = state.selectedType,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RecommendationList(
    recommendations: List<RecommendedAnime>,
    selectedType: RecommendationType,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = MaterialTheme.spacing.screenHorizontal,
                vertical = MaterialTheme.spacing.small,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(recommendations, key = { it.animeId }) { anime ->
            RecommendationCard(
                anime = anime,
                selectedType = selectedType,
            )
        }
    }
}

@Composable
private fun RecommendationGrid(
    recommendations: List<RecommendedAnime>,
    selectedType: RecommendationType,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = MaterialTheme.spacing.screenHorizontal,
                vertical = MaterialTheme.spacing.small,
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(recommendations, key = { it.animeId }) { anime ->
            RecommendationCard(
                anime = anime,
                selectedType = selectedType,
            )
        }
    }
}

@StandardPreviews
@Composable
fun RecommendationScreenPreview(
    @PreviewParameter(RecommendationUiStatePreviewParameterProvider::class)
    state: RecommendationUiState,
) {
    MyAnimeListTheme {
        RecommendationContent(
            uiState = state,
            onEvent = {},
        )
    }
}
