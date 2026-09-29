package com.gokova.myanimelist.feature.details.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gokova.myanimelist.core.ui.component.AnimePosterPreviewDialog
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.details.R
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.presentation.components.AnimeDetailsHeader
import com.gokova.myanimelist.feature.details.presentation.components.AnimeDetailsMetadata
import com.gokova.myanimelist.feature.details.presentation.components.AnimeDetailsSynopsis
import com.gokova.myanimelist.feature.details.presentation.components.AnimeDetailsTaxonomy
import com.gokova.myanimelist.feature.details.presentation.components.DetailRecommendationsCarousel
import com.gokova.myanimelist.feature.details.presentation.components.RelatedAnimeCarousel

private const val WIDE_PRIMARY_WEIGHT = 0.62f
private const val WIDE_DISCOVERY_WEIGHT = 0.38f

// Details needs to enter its two-pane landscape treatment on phone-sized
// landscape windows too; the rest of the app already treats 600.dp as the
// expanded navigation threshold. Keep a little extra width for the two
// readable columns without requiring a tablet-sized window.
private val WIDE_LAYOUT_BREAKPOINT = 720.dp

@Composable
fun AnimeDetailsScreen(
    onBackClick: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnimeDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AnimeDetailsUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = resources.getString(event.messageRes),
                        duration =
                            if (event.isError) {
                                SnackbarDuration.Long
                            } else {
                                SnackbarDuration.Short
                            },
                    )
                }
                is AnimeDetailsUiEvent.NavigateToDetails -> {
                    onAnimeClick(event.animeId)
                }
                is AnimeDetailsUiEvent.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    AnimeDetailsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions =
            AnimeDetailsActions(
                onBackClick = onBackClick,
                onRefresh = viewModel::onRefresh,
                onRetry = viewModel::onRetry,
                onAddToList = viewModel::onAddToList,
                onAnimeClick = onAnimeClick,
            ),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailsContent(
    uiState: AnimeDetailsUiState,
    snackbarHostState: SnackbarHostState,
    actions: AnimeDetailsActions,
    modifier: Modifier = Modifier,
) {
    var previewPoster by remember { mutableStateOf<AnimeDetails?>(null) }

    Scaffold(
        topBar = {
            AnimeDetailsTopBar(
                title = uiState.details?.displayTitle,
                onBackClick = actions.onBackClick,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when {
                uiState.isLoading && uiState.details == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.details == null && uiState.errorMessageRes != null -> {
                    AnimeDetailsErrorState(
                        messageRes = uiState.errorMessageRes,
                        canRetry = uiState.canRetry,
                        onRetry = actions.onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                uiState.details != null -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = actions.onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        AnimeDetailsBody(
                            details = uiState.details,
                            isAddingToList = uiState.isAddingToList,
                            actions = actions,
                            onPosterClick = { previewPoster = uiState.details },
                        )
                    }
                }
            }
        }
    }

    previewPoster?.let { details ->
        AnimePosterPreviewDialog(
            thumbnailUrl = details.mainPictureMedium,
            largeImageUrl = details.mainPictureLarge,
            title = details.displayTitle,
            onDismiss = { previewPoster = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimeDetailsTopBar(
    title: String?,
    onBackClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = title ?: stringResource(R.string.details_top_bar_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription =
                        stringResource(
                            R.string.details_back_button_content_description,
                        ),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

@Composable
private fun AnimeDetailsBody(
    details: AnimeDetails,
    isAddingToList: Boolean,
    actions: AnimeDetailsActions,
    onPosterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val isWideLayout = maxWidth >= WIDE_LAYOUT_BREAKPOINT
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1280.dp)
                    .padding(horizontal = MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
        ) {
            item {
                if (isWideLayout) {
                    WideAnimeDetailsLayout(
                        details = details,
                        isAddingToList = isAddingToList,
                        actions = actions,
                        onPosterClick = onPosterClick,
                    )
                } else {
                    CompactAnimeDetailsLayout(
                        details = details,
                        isAddingToList = isAddingToList,
                        actions = actions,
                        onPosterClick = onPosterClick,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            }
        }
    }
}

@Composable
private fun WideAnimeDetailsLayout(
    details: AnimeDetails,
    isAddingToList: Boolean,
    actions: AnimeDetailsActions,
    onPosterClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(WIDE_PRIMARY_WEIGHT),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            ) {
                AnimeDetailsHeader(
                    details = details,
                    isAddingToList = isAddingToList,
                    onAddToList = actions.onAddToList,
                    onPosterClick = onPosterClick,
                )
                AnimeDetailsSynopsis(synopsis = details.synopsis)
            }

            Column(
                modifier = Modifier.weight(WIDE_DISCOVERY_WEIGHT),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            ) {
                AnimeDetailsMetadata(details = details)
                AnimeDetailsTaxonomy(genres = details.genres, themes = details.themes)
            }
        }

        RelatedAnimeCarousel(
            relatedAnime = details.relatedAnime,
            onAnimeClick = actions.onAnimeClick,
        )
        DetailRecommendationsCarousel(
            recommendations = details.recommendations,
            onAnimeClick = actions.onAnimeClick,
        )
    }
}

@Composable
private fun CompactAnimeDetailsLayout(
    details: AnimeDetails,
    isAddingToList: Boolean,
    actions: AnimeDetailsActions,
    onPosterClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)) {
        AnimeDetailsHeader(
            details = details,
            isAddingToList = isAddingToList,
            onAddToList = actions.onAddToList,
            onPosterClick = onPosterClick,
        )
        AnimeDetailsSynopsis(synopsis = details.synopsis)
        AnimeDetailsMetadata(details = details)
        AnimeDetailsTaxonomy(genres = details.genres, themes = details.themes)
        RelatedAnimeCarousel(
            relatedAnime = details.relatedAnime,
            onAnimeClick = actions.onAnimeClick,
        )
        DetailRecommendationsCarousel(
            recommendations = details.recommendations,
            onAnimeClick = actions.onAnimeClick,
        )
    }
}

@Composable
private fun AnimeDetailsErrorState(
    messageRes: Int,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (canRetry) {
            Button(onClick = onRetry) {
                Text(stringResource(R.string.details_error_retry))
            }
        }
    }
}

@StandardPreviews
@Composable
fun AnimeDetailsScreenPreview(
    @PreviewParameter(AnimeDetailsPreviewParameterProvider::class) state: AnimeDetailsUiState,
) {
    MyAnimeListTheme {
        AnimeDetailsContent(
            uiState = state,
            snackbarHostState = remember { SnackbarHostState() },
            actions =
                AnimeDetailsActions(
                    onBackClick = {},
                    onRefresh = {},
                    onRetry = {},
                    onAddToList = {},
                    onAnimeClick = {},
                ),
        )
    }
}
