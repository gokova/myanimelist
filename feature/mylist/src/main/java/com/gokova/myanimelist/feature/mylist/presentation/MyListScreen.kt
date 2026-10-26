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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gokova.myanimelist.core.ui.component.AnimePosterPreviewDialog
import com.gokova.myanimelist.core.ui.component.editlist.EditAnimeStatusBottomSheet
import com.gokova.myanimelist.core.ui.component.editlist.EditAnimeStatusSheetActions
import com.gokova.myanimelist.core.ui.component.editlist.withStatus
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SortOption
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.presentation.components.AnimeCard
import com.gokova.myanimelist.feature.mylist.presentation.components.AnimeCardActions
import com.gokova.myanimelist.feature.mylist.presentation.components.MyListCategoryTabs
import com.gokova.myanimelist.feature.mylist.presentation.components.MyListEmptyState
import com.gokova.myanimelist.feature.mylist.presentation.components.MyListHeader
import com.gokova.myanimelist.core.ui.R as CoreUiR

@Composable
fun MyListScreen(
    onAnimeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    MyListEventsEffect(viewModel = viewModel, snackbarHostState = snackbarHostState)

    val sheetActions = rememberMyListSheetActions(viewModel = viewModel)

    MyListContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions =
            MyListActions(
                onCategorySelected = viewModel::onCategorySelected,
                onSortSelected = viewModel::onSortOptionSelected,
                onRefresh = { viewModel.onSync(isPullToRefresh = true) },
                onAnimeClick = onAnimeClick,
                onQuickIncrement = viewModel::onQuickIncrement,
                onEditClick = viewModel::setEditingAnime,
                onSheetActions = sheetActions,
            ),
        modifier = modifier,
    )
}

@Composable
private fun MyListEventsEffect(
    viewModel: MyListViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MyListUiEvent.ShowSnackbar -> {
                    val message = resources.getString(event.messageRes)
                    val actionLabel = event.actionRes?.let { resources.getString(it) }
                    val duration =
                        if (event.isError) {
                            SnackbarDuration.Long
                        } else {
                            SnackbarDuration.Short
                        }
                    val result =
                        snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = actionLabel,
                            duration = duration,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onSync(force = true)
                    }
                }
                is MyListUiEvent.ShowCompletionPrompt -> {
                    showPromptSnackbar(
                        snackbarHostState = snackbarHostState,
                        message =
                            resources.getString(
                                CoreUiR.string.edit_list_status_completed_prompt,
                            ),
                        actionLabel =
                            resources.getString(
                                CoreUiR.string.edit_list_status_completed_action,
                            ),
                        onAction = { viewModel.onMarkStatus(event.animeId, "completed") },
                    )
                }
                is MyListUiEvent.ShowWatchingPrompt -> {
                    showPromptSnackbar(
                        snackbarHostState = snackbarHostState,
                        message =
                            resources.getString(
                                CoreUiR.string.edit_list_status_watching_prompt,
                            ),
                        actionLabel =
                            resources.getString(
                                CoreUiR.string.edit_list_status_watching_action,
                            ),
                        onAction = { viewModel.onMarkStatus(event.animeId, "watching") },
                    )
                }
            }
        }
    }
}

private suspend fun showPromptSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val result =
        snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long,
        )
    if (result == SnackbarResult.ActionPerformed) {
        onAction()
    }
}

@Composable
private fun rememberMyListSheetActions(viewModel: MyListViewModel): EditAnimeStatusSheetActions =
    remember(viewModel) {
        EditAnimeStatusSheetActions(
            onStatusChange = { status ->
                viewModel.updateEditingSheet { it.withStatus(status) }
            },
            onEpisodesChange = { ep ->
                viewModel.updateEditingSheet { it.copy(episodesWatched = ep) }
            },
            onScoreChange = { score ->
                viewModel.updateEditingSheet { it.copy(score = score) }
            },
            onRewatchingChange = { rewatching ->
                viewModel.updateEditingSheet { it.copy(isRewatching = rewatching) }
            },
            onSave = viewModel::onSaveEditSheet,
            onRequestDelete = {
                viewModel.updateEditingSheet { it.copy(showDeleteConfirmation = true) }
            },
            onConfirmDelete = viewModel::onConfirmDeleteFromList,
            onDismissDeleteConfirmation = {
                viewModel.updateEditingSheet { it.copy(showDeleteConfirmation = false) }
            },
            onDismiss = { viewModel.setEditingAnime(null) },
        )
    }

data class MyListActions(
    val onCategorySelected: (ListFilterCategory) -> Unit,
    val onSortSelected: (SortOption) -> Unit,
    val onRefresh: () -> Unit,
    val onAnimeClick: (Long) -> Unit = {},
    val onQuickIncrement: (UserAnime) -> Unit = {},
    val onEditClick: (UserAnime) -> Unit = {},
    val onSheetActions: EditAnimeStatusSheetActions? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListContent(
    uiState: MyListUiState,
    snackbarHostState: SnackbarHostState,
    actions: MyListActions,
    modifier: Modifier = Modifier,
) {
    var previewAnime by remember { mutableStateOf<UserAnime?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
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
                    actions = actions,
                    onPosterClick = { previewAnime = it },
                )
            }
        }
    }

    previewAnime?.let { anime ->
        AnimePosterPreviewDialog(
            thumbnailUrl = anime.thumbnailUrl,
            largeImageUrl = anime.largeImageUrl,
            title = anime.displayTitle,
            onDismiss = { previewAnime = null },
        )
    }

    if (uiState.editingSheetState != null && actions.onSheetActions != null) {
        EditAnimeStatusBottomSheet(
            state = uiState.editingSheetState,
            actions = actions.onSheetActions,
        )
    }
}

private val EXPANDED_WIDTH_BREAKPOINT = 600.dp

@Composable
private fun MyListBody(
    uiState: MyListUiState,
    actions: MyListActions,
    onPosterClick: (UserAnime) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.animeList.isEmpty() && !uiState.isLoadingInitial) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            MyListEmptyState(
                onSyncClick = actions.onRefresh,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.huge),
            )
        }
    } else if (uiState.animeList.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else {
        val windowInfo = LocalWindowInfo.current
        val density = LocalDensity.current
        val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
        val isExpanded = screenWidthDp >= EXPANDED_WIDTH_BREAKPOINT

        if (isExpanded) {
            MyListGrid(
                uiState = uiState,
                actions = actions,
                onPosterClick = onPosterClick,
                modifier = modifier,
            )
        } else {
            MyListColumn(
                uiState = uiState,
                actions = actions,
                onPosterClick = onPosterClick,
                modifier = modifier,
            )
        }
    }
}

private val GRID_CELL_MIN_SIZE = 300.dp

@Composable
private fun MyListGrid(
    uiState: MyListUiState,
    actions: MyListActions,
    onPosterClick: (UserAnime) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(
            items = uiState.animeList,
            key = { it.id },
        ) { anime ->
            AnimeCard(
                anime = anime,
                actions =
                    AnimeCardActions(
                        onPosterClick = { onPosterClick(anime) },
                        onCardClick = { actions.onAnimeClick(anime.id) },
                        onEditClick = { actions.onEditClick(anime) },
                        onQuickIncrement = { actions.onQuickIncrement(anime) },
                    ),
                isSavingProgress = uiState.savingProgressAnimeIds.contains(anime.id),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MyListColumn(
    uiState: MyListUiState,
    actions: MyListActions,
    onPosterClick: (UserAnime) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        items(
            items = uiState.animeList,
            key = { it.id },
        ) { anime ->
            AnimeCard(
                anime = anime,
                actions =
                    AnimeCardActions(
                        onPosterClick = { onPosterClick(anime) },
                        onCardClick = { actions.onAnimeClick(anime.id) },
                        onEditClick = { actions.onEditClick(anime) },
                        onQuickIncrement = { actions.onQuickIncrement(anime) },
                    ),
                isSavingProgress = uiState.savingProgressAnimeIds.contains(anime.id),
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
        Surface(color = MaterialTheme.colorScheme.background) {
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
}
