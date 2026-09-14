package com.gokova.myanimelist.feature.taste.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.taste.R
import com.gokova.myanimelist.feature.taste.presentation.components.PackedBubbleChart
import com.gokova.myanimelist.feature.taste.presentation.components.PackedBubbleChartActions
import com.gokova.myanimelist.feature.taste.presentation.components.TasteBottomSheet
import com.gokova.myanimelist.feature.taste.presentation.components.TasteEmptyState
import com.gokova.myanimelist.feature.taste.presentation.components.TasteTypeSelector

@Composable
fun TasteScreen(
    modifier: Modifier = Modifier,
    viewModel: TasteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TasteContent(
        uiState = uiState,
        actions =
            TasteActions(
                onTypeSelected = viewModel::onTypeSelected,
                onBubbleClick = viewModel::onBubbleSelected,
                onRecenterClick = viewModel::onRecenterClicked,
                onDismissBottomSheet = viewModel::onDismissBottomSheet,
            ),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasteContent(
    uiState: TasteUiState,
    actions: TasteActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.screenHorizontal),
                contentAlignment = Alignment.Center,
            ) {
                TasteTypeSelector(
                    selectedType = uiState.selectedType,
                    onTypeSelected = actions.onTypeSelected,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            TasteContentBody(
                uiState = uiState,
                actions = actions,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }

        uiState.selectedBubble?.let { bubble ->
            TasteBottomSheet(
                bubble = bubble,
                onDismissRequest = actions.onDismissBottomSheet,
            )
        }
    }
}

@Composable
private fun TasteContentBody(
    uiState: TasteUiState,
    actions: TasteActions,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
            }
            uiState.isEmptyList -> {
                TasteEmptyState(
                    titleRes = R.string.taste_empty_list_title,
                    subtitleRes = R.string.taste_empty_list_subtitle,
                )
            }
            uiState.isEmptyEligible -> {
                TasteEmptyState(
                    titleRes = R.string.taste_empty_eligible_title,
                    subtitleRes = R.string.taste_empty_eligible_subtitle,
                )
            }
            else -> {
                PackedBubbleChart(
                    bubbles = uiState.currentBubbles,
                    actions =
                        PackedBubbleChartActions(
                            onBubbleClick = actions.onBubbleClick,
                            onRecenterClick = actions.onRecenterClick,
                        ),
                    recenterTrigger = uiState.recenterTrigger,
                    selectedBubble = uiState.selectedBubble,
                )
            }
        }
    }
}

@StandardPreviews
@Composable
private fun TasteScreenPreview(
    @PreviewParameter(TasteUiStatePreviewParameterProvider::class)
    uiState: TasteUiState,
) {
    MyAnimeListTheme {
        TasteContent(
            uiState = uiState,
            actions =
                TasteActions(
                    onTypeSelected = {},
                    onBubbleClick = {},
                    onRecenterClick = {},
                    onDismissBottomSheet = {},
                ),
        )
    }
}
