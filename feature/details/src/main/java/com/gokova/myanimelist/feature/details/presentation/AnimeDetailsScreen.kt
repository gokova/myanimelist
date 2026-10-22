package com.gokova.myanimelist.feature.details.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.material3.SnackbarResult
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
import com.gokova.myanimelist.core.ui.component.editlist.EditAnimeStatusBottomSheet
import com.gokova.myanimelist.core.ui.component.editlist.EditAnimeStatusSheetActions
import com.gokova.myanimelist.core.ui.component.editlist.withStatus
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.details.R
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.presentation.components.AnimeDetailsHeaderState
import com.gokova.myanimelist.feature.details.presentation.components.CompactAnimeDetailsLayout
import com.gokova.myanimelist.feature.details.presentation.components.WideAnimeDetailsLayout
import com.gokova.myanimelist.core.ui.R as CoreUiR

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

    AnimeDetailsEventsEffect(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onAnimeClick = onAnimeClick,
    )

    val sheetActions = rememberAnimeDetailsSheetActions(viewModel = viewModel)

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
                onEditStatusClick = { viewModel.setEditingSheet(open = true) },
                onToggleNotInterested = viewModel::onToggleNotInterested,
                onSheetActions = sheetActions,
            ),
        modifier = modifier,
    )
}

@Composable
private fun AnimeDetailsEventsEffect(
    viewModel: AnimeDetailsViewModel,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onAnimeClick: (Long) -> Unit,
) {
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
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
                is AnimeDetailsUiEvent.ShowCompletionPrompt -> {
                    showActionSnackbar(
                        snackbarHostState = snackbarHostState,
                        message =
                            resources.getString(
                                CoreUiR.string.edit_list_status_completed_prompt,
                            ),
                        actionLabel =
                            resources.getString(
                                CoreUiR.string.edit_list_status_completed_action,
                            ),
                        onAction = { viewModel.onMarkStatus("completed") },
                    )
                }
                is AnimeDetailsUiEvent.ShowWatchingPrompt -> {
                    showActionSnackbar(
                        snackbarHostState = snackbarHostState,
                        message =
                            resources.getString(
                                CoreUiR.string.edit_list_status_watching_prompt,
                            ),
                        actionLabel =
                            resources.getString(
                                CoreUiR.string.edit_list_status_watching_action,
                            ),
                        onAction = { viewModel.onMarkStatus("watching") },
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
}

private suspend fun showActionSnackbar(
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
private fun rememberAnimeDetailsSheetActions(
    viewModel: AnimeDetailsViewModel,
): EditAnimeStatusSheetActions =
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
            onDismiss = { viewModel.setEditingSheet(open = false) },
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
                canToggleNotInterested = uiState.details?.let { !it.isInUserList } == true,
                isNotInterested = uiState.isNotInterested,
                onBackClick = actions.onBackClick,
                onToggleNotInterested = actions.onToggleNotInterested,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        AnimeDetailsContentBody(
            uiState = uiState,
            actions = actions,
            onPosterClick = { previewPoster = it },
            modifier = Modifier.padding(paddingValues),
        )
    }

    previewPoster?.let { details ->
        AnimePosterPreviewDialog(
            thumbnailUrl = details.mainPictureMedium,
            largeImageUrl = details.mainPictureLarge,
            title = details.displayTitle,
            onDismiss = { previewPoster = null },
        )
    }

    if (uiState.editingSheetState != null && actions.onSheetActions != null) {
        EditAnimeStatusBottomSheet(
            state = uiState.editingSheetState,
            actions = actions.onSheetActions,
        )
    }
}

@Composable
private fun AnimeDetailsContentBody(
    uiState: AnimeDetailsUiState,
    actions: AnimeDetailsActions,
    onPosterClick: (AnimeDetails) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
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
                        uiState = uiState,
                        actions = actions,
                        onPosterClick = { onPosterClick(uiState.details) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimeDetailsTopBar(
    title: String?,
    canToggleNotInterested: Boolean,
    isNotInterested: Boolean,
    onBackClick: () -> Unit,
    onToggleNotInterested: () -> Unit,
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
        actions = {
            if (canToggleNotInterested) {
                IconButton(onClick = onToggleNotInterested) {
                    Icon(
                        imageVector =
                            if (isNotInterested) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Outlined.VisibilityOff
                            },
                        contentDescription =
                            stringResource(
                                if (isNotInterested) {
                                    R.string.details_action_remove_not_interested
                                } else {
                                    R.string.details_action_mark_not_interested
                                },
                            ),
                        tint =
                            if (isNotInterested) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
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
    uiState: AnimeDetailsUiState,
    actions: AnimeDetailsActions,
    onPosterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = uiState.details ?: return
    val headerState =
        remember(uiState.isAddingToList, uiState.isNotInterested) {
            AnimeDetailsHeaderState(
                isAddingToList = uiState.isAddingToList,
                isNotInterested = uiState.isNotInterested,
            )
        }
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
                        headerState = headerState,
                        actions = actions,
                        onPosterClick = onPosterClick,
                    )
                } else {
                    CompactAnimeDetailsLayout(
                        details = details,
                        headerState = headerState,
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
