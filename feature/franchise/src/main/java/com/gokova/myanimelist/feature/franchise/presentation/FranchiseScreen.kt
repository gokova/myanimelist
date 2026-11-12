package com.gokova.myanimelist.feature.franchise.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.franchise.R
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraphLayout
import com.gokova.myanimelist.feature.franchise.presentation.components.FranchiseCanvas
import com.gokova.myanimelist.feature.franchise.presentation.components.FranchiseControlsOverlay
import com.gokova.myanimelist.feature.franchise.presentation.components.FranchiseListView
import com.gokova.myanimelist.feature.franchise.presentation.components.FranchiseNodeBottomSheet
import com.gokova.myanimelist.feature.franchise.presentation.components.FranchiseViewportTriggers

@Composable
fun FranchiseScreen(
    onAnimeClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FranchiseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is FranchiseUiEvent.NavigateToDetails -> onAnimeClick(event.animeId)
                is FranchiseUiEvent.NavigateBack -> onBackClick()
                is FranchiseUiEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(resources.getString(event.messageRes))
            }
        }
    }

    val actions =
        remember(viewModel) {
            FranchiseActions(
                onBackClick = viewModel::onBackClick,
                onToggleViewMode = viewModel::onToggleViewMode,
                onNodeClick = viewModel::onNodeClick,
                onDismissBottomSheet = viewModel::onDismissBottomSheet,
                onOpenDetails = viewModel::onOpenDetails,
                onIncrementEpisode = viewModel::onIncrementEpisode,
                onUnignoreAnime = viewModel::onUnignoreAnime,
                onRecenterClick = viewModel::onRecenterClick,
                onFitClick = viewModel::onFitClick,
                onZoomInClick = viewModel::onZoomInClick,
                onZoomOutClick = viewModel::onZoomOutClick,
            )
        }

    FranchiseContent(
        uiState = uiState,
        actions = actions,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun FranchiseContent(
    uiState: FranchiseUiState,
    actions: FranchiseActions,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            FranchiseTopBar(
                viewMode = uiState.viewMode,
                onBackClick = actions.onBackClick,
                onToggleViewMode = actions.onToggleViewMode,
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
                uiState.isLoading && uiState.graph == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.graph?.nodes.isNullOrEmpty() -> {
                    FranchiseEmptyState(modifier = Modifier.align(Alignment.Center))
                }
                uiState.viewMode == FranchiseViewMode.MAP && uiState.layout != null -> {
                    FranchiseMapViewport(
                        layout = uiState.layout,
                        uiState = uiState,
                        actions = actions,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    FranchiseListView(
                        graph = checkNotNull(uiState.graph),
                        onNodeClick = actions.onNodeClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    if (uiState.selectedNode != null) {
        FranchiseNodeBottomSheet(
            node = uiState.selectedNode,
            isUpdatingEpisode = uiState.isUpdatingEpisode,
            actions = actions,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FranchiseTopBar(
    viewMode: FranchiseViewMode,
    onBackClick: () -> Unit,
    onToggleViewMode: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.franchise_top_bar_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        actions = {
            val isMapView = viewMode == FranchiseViewMode.MAP
            IconButton(onClick = onToggleViewMode) {
                Icon(
                    imageVector =
                        if (isMapView) {
                            Icons.AutoMirrored.Filled.List
                        } else {
                            Icons.Default.AccountTree
                        },
                    contentDescription =
                        stringResource(
                            if (isMapView) {
                                R.string.franchise_action_switch_to_list
                            } else {
                                R.string.franchise_action_switch_to_map
                            },
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
private fun FranchiseMapViewport(
    layout: FranchiseGraphLayout,
    uiState: FranchiseUiState,
    actions: FranchiseActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        FranchiseCanvas(
            layout = layout,
            viewportTriggers =
                FranchiseViewportTriggers(
                    recenter = uiState.recenterTrigger,
                    fit = uiState.fitTrigger,
                    zoomIn = uiState.zoomInTrigger,
                    zoomOut = uiState.zoomOutTrigger,
                ),
            onNodeClick = actions.onNodeClick,
            modifier = Modifier.fillMaxSize(),
        )

        FranchiseControlsOverlay(
            onRecenterClick = actions.onRecenterClick,
            onFitClick = actions.onFitClick,
            onZoomInClick = actions.onZoomInClick,
            onZoomOutClick = actions.onZoomOutClick,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun FranchiseEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.small),
        )
        Text(
            text = stringResource(R.string.franchise_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            text = stringResource(R.string.franchise_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
