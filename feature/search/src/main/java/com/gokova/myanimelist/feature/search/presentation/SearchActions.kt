package com.gokova.myanimelist.feature.search.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem

data class SearchItemActions(
    val onAnimeClick: (Long) -> Unit = {},
    val onPosterClick: (SearchAnimeItem) -> Unit = {},
)

data class MissingKeyActions(
    val onDismiss: () -> Unit = {},
    val onConfigureKey: () -> Unit = {},
)

data class AiSearchActions(
    val onPromptChange: (String) -> Unit = {},
    val onSubmitPrompt: () -> Unit = {},
    val onClearPrompt: () -> Unit = {},
    val onShowInfoDialog: () -> Unit = {},
    val onDismissInfoDialog: () -> Unit = {},
    val missingKeyActions: MissingKeyActions = MissingKeyActions(),
)

data class StandardSearchActions(
    val onQueryChange: (String) -> Unit = {},
    val onSearch: () -> Unit = {},
    val onClearQuery: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onRetrySearch: () -> Unit = {},
)

data class SearchActions(
    val onBackClick: () -> Unit = {},
    val onToggleMode: () -> Unit = {},
    val standardActions: StandardSearchActions = StandardSearchActions(),
    val aiActions: AiSearchActions = AiSearchActions(),
)

@Composable
fun rememberSearchItemActions(
    onAnimeClick: (Long) -> Unit,
    onPosterClick: (SearchAnimeItem) -> Unit,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
): SearchItemActions =
    remember(onAnimeClick, onPosterClick, focusManager, keyboardController) {
        SearchItemActions(
            onAnimeClick = { animeId ->
                focusManager.clearFocus()
                keyboardController?.hide()
                onAnimeClick(animeId)
            },
            onPosterClick = { anime ->
                focusManager.clearFocus()
                keyboardController?.hide()
                onPosterClick(anime)
            },
        )
    }

@Composable
fun rememberSearchActions(
    viewModel: SearchViewModel,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
    onBackClick: () -> Unit,
): SearchActions =
    remember(viewModel, focusManager, keyboardController, onBackClick) {
        SearchActions(
            onBackClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onBackClick()
            },
            onToggleMode = viewModel::onToggleMode,
            standardActions =
                StandardSearchActions(
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = viewModel::onSearch,
                    onClearQuery = viewModel::onClearQuery,
                    onLoadMore = viewModel::onLoadMore,
                    onRetrySearch = viewModel::onRetrySearch,
                ),
            aiActions =
                AiSearchActions(
                    onPromptChange = viewModel::onAiPromptChange,
                    onSubmitPrompt = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        viewModel.onAiSearch()
                    },
                    onClearPrompt = { viewModel.onAiPromptChange("") },
                    onShowInfoDialog = { viewModel.setAiInfoDialogVisible(true) },
                    onDismissInfoDialog = { viewModel.setAiInfoDialogVisible(false) },
                    missingKeyActions =
                        MissingKeyActions(
                            onDismiss = { viewModel.setMissingKeyDialogVisible(false) },
                            onConfigureKey = viewModel::onConfigureKeyClicked,
                        ),
                ),
        )
    }
