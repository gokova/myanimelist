package com.gokova.myanimelist.feature.mylist.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SortOption
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.presentation.components.AnimeCard
import com.gokova.myanimelist.feature.mylist.presentation.components.MyListCategoryTabs
import com.gokova.myanimelist.feature.mylist.presentation.components.MyListEmptyState
import com.gokova.myanimelist.feature.mylist.presentation.components.MyListHeader

@Composable
fun MyListScreen(
    modifier: Modifier = Modifier,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MyListUiEvent.ShowSnackbar -> {
                    val message = resources.getString(event.messageRes)
                    val actionLabel = event.actionRes?.let { resources.getString(it) }
                    val result =
                        snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = actionLabel,
                            duration =
                                if (event.isError) {
                                    SnackbarDuration.Long
                                } else {
                                    SnackbarDuration.Short
                                },
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onRetrySync()
                    }
                }
            }
        }
    }

    MyListContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions =
            MyListActions(
                onCategorySelected = viewModel::onCategorySelected,
                onSortSelected = viewModel::onSortOptionSelected,
                onRefresh = viewModel::onRefresh,
            ),
        modifier = modifier,
    )
}

data class MyListActions(
    val onCategorySelected: (ListFilterCategory) -> Unit,
    val onSortSelected: (SortOption) -> Unit,
    val onRefresh: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListContent(
    uiState: MyListUiState,
    snackbarHostState: SnackbarHostState,
    actions: MyListActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MyListCategoryTabs(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = actions.onCategorySelected,
                )

                MyListHeader(
                    totalCount = uiState.animeList.size,
                    selectedSort = uiState.selectedSort,
                    onSortSelected = actions.onSortSelected,
                )

                MyListBody(
                    uiState = uiState,
                    onRefresh = actions.onRefresh,
                )
            }
        }
    }
}

private val EXPANDED_WIDTH_BREAKPOINT = 600.dp

@Composable
private fun MyListBody(
    uiState: MyListUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.animeList.isEmpty() && !uiState.isLoadingInitial) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            MyListEmptyState(
                onSyncClick = onRefresh,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.huge),
            )
        }
    } else {
        val windowInfo = LocalWindowInfo.current
        val density = LocalDensity.current
        val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
        val isExpanded = screenWidthDp >= EXPANDED_WIDTH_BREAKPOINT

        if (isExpanded) {
            MyListGrid(
                animeList = uiState.animeList,
                modifier = modifier,
            )
        } else {
            MyListColumn(
                animeList = uiState.animeList,
                modifier = modifier,
            )
        }
    }
}

private val GRID_CELL_MIN_SIZE = 240.dp

@Composable
private fun MyListGrid(
    animeList: List<UserAnime>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = MaterialTheme.spacing.screenHorizontal,
                end = MaterialTheme.spacing.screenHorizontal,
                top = MaterialTheme.spacing.extraSmall,
                bottom = MaterialTheme.spacing.large,
            ),
        verticalArrangement =
            Arrangement.spacedBy(MaterialTheme.spacing.medium),
        horizontalArrangement =
            Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(
            items = animeList,
            key = { it.id },
        ) { anime ->
            AnimeCard(
                anime = anime,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MyListColumn(
    animeList: List<UserAnime>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = MaterialTheme.spacing.screenHorizontal,
                end = MaterialTheme.spacing.screenHorizontal,
                top = MaterialTheme.spacing.extraSmall,
                bottom = MaterialTheme.spacing.large,
            ),
        verticalArrangement =
            Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(
            items = animeList,
            key = { it.id },
        ) { anime ->
            AnimeCard(
                anime = anime,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@StandardPreviews
@Composable
private fun MyListScreenPreview(
    @PreviewParameter(MyListUiStatePreviewParameterProvider::class) uiState: MyListUiState,
) {
    MyAnimeListTheme {
        MyListContent(
            uiState = uiState,
            snackbarHostState = remember { SnackbarHostState() },
            actions =
                MyListActions(
                    onCategorySelected = {},
                    onSortSelected = {},
                    onRefresh = {},
                ),
        )
    }
}
