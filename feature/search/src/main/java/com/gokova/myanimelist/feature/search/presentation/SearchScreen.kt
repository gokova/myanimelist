package com.gokova.myanimelist.feature.search.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.presentation.components.SearchAnimeCard
import com.gokova.myanimelist.feature.search.presentation.components.SearchEmptyState
import com.gokova.myanimelist.feature.search.presentation.components.SearchErrorState
import com.gokova.myanimelist.feature.search.presentation.components.SearchTopBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

private val EXPANDED_WIDTH_BREAKPOINT = 600.dp
private val GRID_CELL_MIN_SIZE = 300.dp
private const val PAGINATION_BUFFER = 3

@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
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
            }
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val actions =
        SearchActions(
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::onSearch,
            onClearQuery = viewModel::onClearQuery,
            onLoadMore = viewModel::onLoadMore,
            onRetrySearch = viewModel::onRetrySearch,
            onBackClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onBackClick()
            },
            onAnimeClick = { animeId ->
                focusManager.clearFocus()
                keyboardController?.hide()
                onAnimeClick(animeId)
            },
        )

    SearchContent(
        uiState = uiState,
        actions = actions,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun SearchContent(
    uiState: SearchUiState,
    actions: SearchActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val focusRequester = remember { FocusRequester() }
    var hasAutoFocused by rememberSaveable { mutableStateOf(false) }
    val isInspection = LocalInspectionMode.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val isExpanded = screenWidthDp >= EXPANDED_WIDTH_BREAKPOINT

    LaunchedEffect(Unit) {
        if (!isInspection && !hasAutoFocused && uiState.query.isEmpty()) {
            hasAutoFocused = true
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SearchTopBar(
                query = uiState.query,
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
            )
        }
    }
}

@Composable
private fun SearchBody(
    uiState: SearchUiState,
    isExpanded: Boolean,
    actions: SearchActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is SearchUiState.Initial -> {
                SearchEmptyState(
                    isInitial = true,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is SearchUiState.Searching -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is SearchUiState.Empty -> {
                SearchEmptyState(
                    isInitial = false,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is SearchUiState.Error -> {
                SearchErrorState(
                    onRetry = actions.onRetrySearch,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is SearchUiState.Content -> {
                SearchContentResults(
                    state = uiState,
                    isExpanded = isExpanded,
                    actions = actions,
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
    modifier: Modifier = Modifier,
) {
    if (isExpanded) {
        SearchGridResults(state = state, actions = actions, modifier = modifier)
    } else {
        SearchListResults(state = state, actions = actions, modifier = modifier)
    }
}

@Composable
private fun SearchListResults(
    state: SearchUiState.Content,
    actions: SearchActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    ObserveListPagination(listState, state, actions.onLoadMore)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = MaterialTheme.spacing.screenHorizontal,
                vertical = MaterialTheme.spacing.medium,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(state.animes, key = { it.id }) { anime ->
            SearchAnimeCard(anime = anime, onCardClick = actions.onAnimeClick)
        }
        if (state.isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        } else if (state.loadMoreError != null) {
            item(key = "load_more_error") {
                LoadMoreErrorFooter(onRetry = actions.onLoadMore)
            }
        }
    }
}

@Composable
private fun SearchGridResults(
    state: SearchUiState.Content,
    actions: SearchActions,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    ObserveGridPagination(gridState, state, actions.onLoadMore)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = MaterialTheme.spacing.screenHorizontal,
                vertical = MaterialTheme.spacing.medium,
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(state.animes, key = { it.id }) { anime ->
            SearchAnimeCard(anime = anime, onCardClick = actions.onAnimeClick)
        }
        if (state.isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "grid_loading_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        } else if (state.loadMoreError != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "grid_load_more_error") {
                LoadMoreErrorFooter(onRetry = actions.onLoadMore)
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
    LaunchedEffect(listState, state.animes.size, state.hasMore, state.isLoadingMore) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                val threshold = state.animes.size - PAGINATION_BUFFER
                if (lastVisibleIndex >= threshold && state.hasMore && !state.isLoadingMore) {
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
    LaunchedEffect(gridState, state.animes.size, state.hasMore, state.isLoadingMore) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                val threshold = state.animes.size - PAGINATION_BUFFER
                if (lastVisibleIndex >= threshold && state.hasMore && !state.isLoadingMore) {
                    onLoadMore()
                }
            }
    }
}

@Composable
private fun LoadMoreErrorFooter(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium)
                .clickable(onClick = onRetry),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.search_load_more_error),
            style = MaterialTheme.typography.bodySmall,
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
            actions =
                SearchActions(
                    onQueryChange = {},
                    onSearch = {},
                    onClearQuery = {},
                    onLoadMore = {},
                    onRetrySearch = {},
                    onBackClick = {},
                    onAnimeClick = {},
                ),
        )
    }
}
