package com.gokova.myanimelist.feature.recommendation.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gokova.myanimelist.core.ui.component.AnimePosterPreviewDialog
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.recommendation.R
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.presentation.components.HeaderSortConfig
import com.gokova.myanimelist.feature.recommendation.presentation.components.NotInterestedBottomSheet
import com.gokova.myanimelist.feature.recommendation.presentation.components.NotInterestedSheetData
import com.gokova.myanimelist.feature.recommendation.presentation.components.PosterPreviewData
import com.gokova.myanimelist.feature.recommendation.presentation.components.RecommendationBodyContent
import com.gokova.myanimelist.feature.recommendation.presentation.components.RecommendationEmptyState
import com.gokova.myanimelist.feature.recommendation.presentation.components.RecommendationHeader
import com.gokova.myanimelist.feature.recommendation.presentation.components.RecommendationItemActions
import com.gokova.myanimelist.feature.recommendation.presentation.components.RecommendationPillSelector

private val EXPANDED_WIDTH_BREAKPOINT = 600.dp

@Composable
fun RecommendationScreen(
    modifier: Modifier = Modifier,
    onAnimeClick: (Long) -> Unit = {},
    viewModel: RecommendationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    RecommendationSideEffects(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
    )

    RecommendationContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onAnimeClick = onAnimeClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
private fun RecommendationSideEffects(
    viewModel: RecommendationViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is RecommendationSideEffect.ShowUndoNotInterestedSnackbar -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result =
                        snackbarHostState.showSnackbar(
                            message =
                                resources.getString(
                                    R.string.recommendation_snackbar_marked_not_interested,
                                    effect.animeTitle,
                                ),
                            actionLabel =
                                resources.getString(
                                    R.string.recommendation_snackbar_undo,
                                ),
                            duration = SnackbarDuration.Short,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(
                            RecommendationUiEvent.UndoNotInterested(effect.animeId),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationContent(
    uiState: RecommendationUiState,
    onEvent: (RecommendationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onAnimeClick: (Long) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var previewPoster by remember { mutableStateOf<PosterPreviewData?>(null) }
    var activeNotInterestedAnime by remember { mutableStateOf<NotInterestedSheetData?>(null) }
    val itemActions =
        remember(onAnimeClick) {
            RecommendationItemActions(
                onPosterClick = { previewPoster = it },
                onAnimeClick = onAnimeClick,
                onNotInterested = { activeNotInterestedAnime = it },
            )
        }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            RecommendationPillHeader(
                selectedType = uiState.selectedType,
                onSelectType = { onEvent(RecommendationUiEvent.SelectType(it)) },
            )

            RecommendationStateContent(
                uiState = uiState,
                onEvent = onEvent,
                itemActions = itemActions,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    RecommendationOverlays(
        previewPoster = previewPoster,
        activeNotInterestedAnime = activeNotInterestedAnime,
        onDismissPreview = { previewPoster = null },
        onDismissNotInterested = { activeNotInterestedAnime = null },
        onMarkNotInterested = { sheetData ->
            onEvent(
                RecommendationUiEvent.MarkNotInterested(
                    animeId = sheetData.animeId,
                    title = sheetData.title,
                    titleEnglish = sheetData.titleEnglish,
                    pictureUrl = sheetData.pictureUrl,
                ),
            )
            activeNotInterestedAnime = null
        },
    )
}

@Composable
private fun RecommendationPillHeader(
    selectedType: RecommendationType,
    onSelectType: (RecommendationType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.screenHorizontal,
                    vertical = MaterialTheme.spacing.small,
                ),
        contentAlignment = Alignment.Center,
    ) {
        RecommendationPillSelector(
            selectedType = selectedType,
            onTypeSelected = onSelectType,
        )
    }
}

@Composable
private fun RecommendationOverlays(
    previewPoster: PosterPreviewData?,
    activeNotInterestedAnime: NotInterestedSheetData?,
    onDismissPreview: () -> Unit,
    onDismissNotInterested: () -> Unit,
    onMarkNotInterested: (NotInterestedSheetData) -> Unit,
) {
    previewPoster?.let { poster ->
        AnimePosterPreviewDialog(
            thumbnailUrl = poster.thumbnailUrl,
            largeImageUrl = poster.largeImageUrl,
            title = poster.title,
            onDismiss = onDismissPreview,
        )
    }

    activeNotInterestedAnime?.let { sheetData ->
        NotInterestedBottomSheet(
            sheetData = sheetData,
            onNotInterested = { onMarkNotInterested(sheetData) },
            onDismissRequest = onDismissNotInterested,
        )
    }
}

@Composable
private fun RecommendationStateContent(
    uiState: RecommendationUiState,
    onEvent: (RecommendationUiEvent) -> Unit,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (uiState) {
            is RecommendationUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is RecommendationUiState.EmptyInsufficientData -> {
                RecommendationEmptyState(
                    titleRes = R.string.recommendation_empty_insufficient_title,
                    subtitleRes = R.string.recommendation_empty_insufficient_subtitle,
                )
            }
            is RecommendationUiState.EmptyAllCaughtUp -> {
                val isNewSeasons = uiState.selectedType == RecommendationType.NEW_SEASONS
                RecommendationEmptyState(
                    titleRes =
                        if (isNewSeasons) {
                            R.string.new_seasons_empty_all_caught_up_title
                        } else {
                            R.string.recommendation_empty_all_caught_up_title
                        },
                    subtitleRes =
                        if (isNewSeasons) {
                            R.string.new_seasons_empty_all_caught_up_subtitle
                        } else {
                            R.string.recommendation_empty_all_caught_up_subtitle
                        },
                    actionButtonTextRes = R.string.recommendation_btn_calculate_now,
                    onActionClick = { onEvent(RecommendationUiEvent.CalculateNow) },
                )
            }
            is RecommendationUiState.Calculating -> {
                RecommendationCalculatingState(
                    selectedType = uiState.selectedType,
                    onCalculateNow = { onEvent(RecommendationUiEvent.CalculateNow) },
                )
            }
            is RecommendationUiState.Error -> {
                RecommendationEmptyState(
                    titleRes = R.string.recommendation_error_title,
                    subtitleRes = R.string.recommendation_error_subtitle,
                    actionButtonTextRes = R.string.recommendation_btn_retry,
                    onActionClick = { onEvent(RecommendationUiEvent.Refresh) },
                )
            }
            is RecommendationUiState.Success -> {
                RecommendationSuccessContent(
                    state = uiState,
                    onEvent = onEvent,
                    itemActions = itemActions,
                )
            }
        }
    }
}

@Composable
private fun RecommendationCalculatingState(
    selectedType: RecommendationType,
    onCalculateNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNewSeasons = selectedType == RecommendationType.NEW_SEASONS
    RecommendationEmptyState(
        titleRes =
            if (isNewSeasons) {
                R.string.new_seasons_calculating_title
            } else {
                R.string.recommendation_calculating_title
            },
        subtitleRes =
            if (isNewSeasons) {
                R.string.new_seasons_calculating_subtitle
            } else {
                R.string.recommendation_calculating_subtitle
            },
        actionButtonTextRes = R.string.recommendation_btn_calculate_now,
        onActionClick = onCalculateNow,
        modifier = modifier,
    )
}

@Composable
private fun RecommendationSuccessContent(
    state: RecommendationUiState.Success,
    onEvent: (RecommendationUiEvent) -> Unit,
    itemActions: RecommendationItemActions,
    modifier: Modifier = Modifier,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val isExpanded = screenWidthDp >= EXPANDED_WIDTH_BREAKPOINT

    Column(modifier = modifier.fillMaxSize()) {
        val count =
            if (state.selectedType == RecommendationType.NEW_SEASONS) {
                state.newSeasons.size
            } else {
                state.recommendations.size
            }

        val sortConfig =
            if (state.selectedType == RecommendationType.NEW_SEASONS) {
                HeaderSortConfig(
                    selectedSort = state.newSeasonSort,
                    onSortSelected = { onEvent(RecommendationUiEvent.SelectNewSeasonSort(it)) },
                )
            } else {
                null
            }

        RecommendationHeader(
            count = count,
            selectedType = state.selectedType,
            sortConfig = sortConfig,
            onRefreshClick = { onEvent(RecommendationUiEvent.Refresh) },
        )

        RecommendationBodyContent(
            state = state,
            isExpanded = isExpanded,
            itemActions = itemActions,
            modifier = Modifier.weight(1f),
        )
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
