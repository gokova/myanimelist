package com.gokova.myanimelist.feature.search.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem
import com.gokova.myanimelist.feature.search.presentation.components.AiSearchContentResults
import com.gokova.myanimelist.feature.search.presentation.components.AiSearchDialogs
import com.gokova.myanimelist.feature.search.presentation.components.AiSearchEmptyState
import com.gokova.myanimelist.feature.search.presentation.components.AiSearchErrorState
import com.gokova.myanimelist.feature.search.presentation.components.AiSearchLoadingState
import com.gokova.myanimelist.feature.search.presentation.components.SearchAnimeCard
import com.gokova.myanimelist.feature.search.presentation.components.SearchEmptyState
import com.gokova.myanimelist.feature.search.presentation.components.SearchErrorState
import com.gokova.myanimelist.feature.search.presentation.components.SearchPosterPreviewOverlay
import com.gokova.myanimelist.feature.search.presentation.components.SearchTopBar
import kotlinx.coroutines.flow.distinctUntilChanged

private val EXPANDED_WIDTH_BREAKPOINT = 600.dp
private val GRID_CELL_MIN_SIZE = 300.dp
private const val PAGINATION_BUFFER = 3

@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(resources.getString(event.messageRes))
                }
                is SearchUiEvent.NavigateToProfile -> {
                    onNavigateToProfile()
                }
            }
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val actions = rememberSearchActions(viewModel, focusManager, keyboardController, onBackClick)

    SearchContent(
        uiState = uiState,
        actions = actions,
        onAnimeClick = onAnimeClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun SearchContent(
    uiState: SearchUiState,
    actions: SearchActions,
    onAnimeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val focusRequester = remember { FocusRequester() }
    var hasAutoFocused by rememberSaveable { mutableStateOf(false) }
    var previewPoster by remember { mutableStateOf<SearchAnimeItem?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isInspection = LocalInspectionMode.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val isExpanded = screenWidthDp >= EXPANDED_WIDTH_BREAKPOINT

    val itemActions =
        rememberSearchItemActions(
            onAnimeClick = onAnimeClick,
            onPosterClick = { previewPoster = it },
            focusManager = focusManager,
            keyboardController = keyboardController,
        )

    LaunchedEffect(Unit) {
        if (!isInspection && !hasAutoFocused && uiState.query.isEmpty()) {
            hasAutoFocused = true
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets =
            ScaffoldDefaults.contentWindowInsets.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
        topBar = {
            SearchTopBar(
                uiState = uiState,
                actions = actions,
                focusRequester = focusRequester,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            SearchBody(
                uiState = uiState,
                isExpanded = isExpanded,
                actions = actions,
                itemActions = itemActions,
            )
        }
    }

    AiSearchDialogs(uiState = uiState, actions = actions)
    SearchPosterPreviewOverlay(poster = previewPoster, onDismiss = { previewPoster = null })
}

@Composable
private fun SearchBody(
    uiState: SearchUiState,
    isExpanded: Boolean,
    actions: SearchActions,
    itemActions: SearchItemActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is SearchUiState.Initial -> {
                SearchEmptyState(isInitial = true, modifier = Modifier.align(Alignment.Center))
            }
            is SearchUiState.Searching -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is SearchUiState.Empty -> {
                SearchEmptyState(isInitial = false, modifier = Modifier.align(Alignment.Center))
            }
            is SearchUiState.Error -> {
                SearchErrorState(
                    onRetry = actions.standardActions.onRetrySearch,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is SearchUiState.Content -> {
                SearchContentResults(
                    state = uiState,
                    isExpanded = isExpanded,
                    actions = actions,
                    itemActions = itemActions,
                )
            }
            is SearchUiState.AiInitial -> {
                AiSearchEmptyState(isInitial = true, modifier = Modifier.align(Alignment.Center))
            }
            is SearchUiState.AiSearching -> {
                AiSearchLoadingState(modifier = Modifier.align(Alignment.Center))
            }
            is SearchUiState.AiEmpty -> {
                AiSearchEmptyState(isInitial = false, modifier = Modifier.align(Alignment.Center))
            }
            is SearchUiState.AiError -> {
                AiSearchErrorState(
                    state = uiState,
                    onRetry = actions.aiActions.onSubmitPrompt,
                    onConfigureKey = actions.aiActions.missingKeyActions.onConfigureKey,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is SearchUiState.AiContent -> {
                AiSearchContentResults(
                    state = uiState,
                    isExpanded = isExpanded,
                    itemActions = itemActions,
                    onShowInfo = actions.aiActions.onShowInfoDialog,
                )
            }
        }
    }
}

@Composable
private fun SearchContentResults(
    state: SearchUiState.Content,
    isExpanded: Boolean,
    actions: SearchActions,
    itemActions: SearchItemActions,
    modifier: Modifier = Modifier,
) {
    if (isExpanded) {
        SearchGridResults(
            state = state,
            actions = actions,
            itemActions = itemActions,
            modifier = modifier,
        )
    } else {
        SearchListResults(
            state = state,
            actions = actions,
            itemActions = itemActions,
            modifier = modifier,
        )
    }
}

@Composable
private fun SearchListResults(
    state: SearchUiState.Content,
    actions: SearchActions,
    itemActions: SearchItemActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    ObserveListPagination(listState, state, actions.standardActions.onLoadMore)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(state.animes, key = { it.id }) { anime ->
            SearchAnimeCard(
                anime = anime,
                onCardClick = itemActions.onAnimeClick,
                onPosterClick = { itemActions.onPosterClick(anime) },
            )
        }
        if (state.isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = MaterialTheme.spacing.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
        if (state.loadMoreErrorRes != null) {
            item(key = "load_more_error") {
                LoadMoreErrorFooter(
                    messageRes = state.loadMoreErrorRes,
                    onRetry = actions.standardActions.onLoadMore,
                )
            }
        }
    }
}

@Composable
private fun SearchGridResults(
    state: SearchUiState.Content,
    actions: SearchActions,
    itemActions: SearchItemActions,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    ObserveGridPagination(gridState, state, actions.standardActions.onLoadMore)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(state.animes, key = { it.id }) { anime ->
            SearchAnimeCard(
                anime = anime,
                onCardClick = itemActions.onAnimeClick,
                onPosterClick = { itemActions.onPosterClick(anime) },
            )
        }
        if (state.isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "grid_loading_more") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = MaterialTheme.spacing.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
        if (state.loadMoreErrorRes != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "grid_load_more_error") {
                LoadMoreErrorFooter(
                    messageRes = state.loadMoreErrorRes,
                    onRetry = actions.standardActions.onLoadMore,
                )
            }
        }
    }
}

@Composable
private fun ObserveListPagination(
    listState: LazyListState,
    state: SearchUiState.Content,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(listState, state.hasMore, state.isLoadingMore) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val total = layout.totalItemsCount
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - PAGINATION_BUFFER
        }.distinctUntilChanged()
            .collect { shouldLoad ->
                if (shouldLoad && state.hasMore && !state.isLoadingMore) {
                    onLoadMore()
                }
            }
    }
}

@Composable
private fun ObserveGridPagination(
    gridState: LazyGridState,
    state: SearchUiState.Content,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(gridState, state.hasMore, state.isLoadingMore) {
        snapshotFlow {
            val layout = gridState.layoutInfo
            val total = layout.totalItemsCount
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - PAGINATION_BUFFER
        }.distinctUntilChanged()
            .collect { shouldLoad ->
                if (shouldLoad && state.hasMore && !state.isLoadingMore) {
                    onLoadMore()
                }
            }
    }
}

@Composable
private fun LoadMoreErrorFooter(
    @StringRes messageRes: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onRetry)
                .padding(vertical = MaterialTheme.spacing.medium),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

@StandardPreviews
@Composable
private fun SearchScreenPreview(
    @PreviewParameter(SearchPreviewParameterProvider::class) uiState: SearchUiState,
) {
    MyAnimeListTheme {
        SearchContent(
            uiState = uiState,
            actions = SearchActions(),
            onAnimeClick = {},
        )
    }
}
