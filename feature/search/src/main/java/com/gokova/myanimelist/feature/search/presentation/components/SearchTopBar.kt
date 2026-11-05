package com.gokova.myanimelist.feature.search.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.domain.usecase.SearchAnimeUseCase
import com.gokova.myanimelist.feature.search.presentation.AiSearchActions
import com.gokova.myanimelist.feature.search.presentation.SearchActions
import com.gokova.myanimelist.feature.search.presentation.SearchMode
import com.gokova.myanimelist.feature.search.presentation.SearchUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private data class SearchInputActions(
    val onSearch: () -> Unit,
    val onClear: () -> Unit,
    val onBack: () -> Unit,
    val onToggleMode: () -> Unit,
)

@Composable
fun SearchTopBar(
    uiState: SearchUiState,
    actions: SearchActions,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    if (uiState.mode == SearchMode.AI) {
        val isLoading = uiState is SearchUiState.AiSearching
        AiSearchTopBarContainer(
            prompt = uiState.aiPrompt,
            actions = actions.aiActions,
            onBackToStandard = actions.onToggleMode,
            isLoading = isLoading,
            modifier = modifier,
        )
    } else {
        StandardSearchTopBar(
            query = uiState.query,
            actions = actions,
            focusRequester = focusRequester,
            modifier = modifier,
        )
    }
}

@Composable
private fun AiSearchTopBarContainer(
    prompt: String,
    actions: AiSearchActions,
    onBackToStandard: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                    ),
                ).padding(horizontal = MaterialTheme.spacing.medium),
    ) {
        AiSearchPromptCard(
            prompt = prompt,
            actions = actions,
            onBackToStandard = onBackToStandard,
            isLoading = isLoading,
        )
    }
}

@Composable
private fun StandardSearchTopBar(
    query: String,
    actions: SearchActions,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val textFieldState =
        rememberTextFieldState(
            initialText = query,
            initialSelection = TextRange(query.length),
        )
    val latestQuery by rememberUpdatedState(query)

    SyncQueryWithState(
        query = query,
        latestQuery = latestQuery,
        textFieldState = textFieldState,
        onQueryChange = actions.standardActions.onQueryChange,
    )

    val currentText = textFieldState.text.toString()
    val canSearch = currentText.trim().length >= SearchAnimeUseCase.MIN_QUERY_LENGTH
    val showValidationHint = currentText.isNotBlank() && !canSearch
    var attemptedInvalidSubmit by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentText) {
        if (canSearch) {
            attemptedInvalidSubmit = false
        }
    }

    val inputActions =
        rememberSearchInputActions(
            textFieldState = textFieldState,
            latestQuery = latestQuery,
            canSearch = canSearch,
            onInvalidSubmit = { attemptedInvalidSubmit = true },
            actions = actions,
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                    ),
                ).padding(horizontal = MaterialTheme.spacing.medium),
    ) {
        SearchBarContainer(
            textFieldState = textFieldState,
            canSearch = canSearch,
            focusRequester = focusRequester,
            inputActions = inputActions,
        )
        ValidationHint(
            visible = showValidationHint,
            isError = attemptedInvalidSubmit,
        )
    }
}

@Composable
private fun rememberSearchInputActions(
    textFieldState: TextFieldState,
    latestQuery: String,
    canSearch: Boolean,
    onInvalidSubmit: () -> Unit,
    actions: SearchActions,
): SearchInputActions {
    val latestCanSearch by rememberUpdatedState(canSearch)
    val latestOnInvalidSubmit by rememberUpdatedState(onInvalidSubmit)
    val latestActions by rememberUpdatedState(actions)

    return remember(textFieldState) {
        SearchInputActions(
            onSearch = {
                if (latestCanSearch) {
                    latestActions.standardActions.onSearch()
                } else {
                    latestOnInvalidSubmit()
                }
            },
            onClear = {
                textFieldState.clearText()
                if (latestQuery.isNotEmpty()) {
                    latestActions.standardActions.onClearQuery()
                }
            },
            onBack = { latestActions.onBackClick() },
            onToggleMode = { latestActions.onToggleMode() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarContainer(
    textFieldState: TextFieldState,
    canSearch: Boolean,
    focusRequester: FocusRequester,
    inputActions: SearchInputActions,
    modifier: Modifier = Modifier,
) {
    SearchBar(
        inputField = {
            SearchInputField(
                textFieldState = textFieldState,
                canSearch = canSearch,
                focusRequester = focusRequester,
                inputActions = inputActions,
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = modifier.fillMaxWidth(),
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors =
            SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
    ) {}
}

@Composable
private fun SyncQueryWithState(
    query: String,
    latestQuery: String,
    textFieldState: TextFieldState,
    onQueryChange: (String) -> Unit,
) {
    LaunchedEffect(query) {
        val current = textFieldState.text.toString()
        if (current != query) {
            textFieldState.edit {
                replace(0, length, query)
                placeCursorAtEnd()
            }
        }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .filter { it != latestQuery }
            .collect { onQueryChange(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputField(
    textFieldState: TextFieldState,
    canSearch: Boolean,
    focusRequester: FocusRequester,
    inputActions: SearchInputActions,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    SearchBarDefaults.InputField(
        state = textFieldState,
        onSearch = { inputActions.onSearch() },
        expanded = false,
        onExpandedChange = {},
        interactionSource = interactionSource,
        placeholder = {
            Text(
                text = stringResource(R.string.search_placeholder),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        },
        leadingIcon = {
            IconButton(
                onClick = inputActions.onBack,
                modifier = Modifier.size(MaterialTheme.spacing.minTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription =
                        stringResource(R.string.search_back_content_description),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        trailingIcon = {
            SearchTrailingIcons(
                query = textFieldState.text.toString(),
                canSearch = canSearch,
                inputActions = inputActions,
            )
        },
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        textFieldState.edit { placeCursorAtEnd() }
                    }
                },
    )
}

@Composable
private fun ValidationHint(
    visible: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        val color =
            if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        Text(
            text = stringResource(R.string.search_validation_hint),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier =
                modifier.padding(
                    start = MaterialTheme.spacing.large,
                    top = MaterialTheme.spacing.extraSmall,
                ),
        )
    }
}

@Composable
private fun SearchTrailingIcons(
    query: String,
    canSearch: Boolean,
    inputActions: SearchInputActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (query.isNotEmpty()) {
            IconButton(
                onClick = inputActions.onClear,
                modifier = Modifier.size(MaterialTheme.spacing.minTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription =
                        stringResource(R.string.search_clear_content_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(
            onClick = inputActions.onSearch,
            enabled = canSearch,
            modifier = Modifier.size(MaterialTheme.spacing.minTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription =
                    stringResource(R.string.search_action_content_description),
                tint =
                    if (canSearch) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
            )
        }
        IconButton(
            onClick = inputActions.onToggleMode,
            modifier = Modifier.size(MaterialTheme.spacing.minTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription =
                    stringResource(R.string.ai_search_mode_toggle_cd),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@StandardPreviews
@Composable
private fun SearchTopBarPreview() {
    MyAnimeListTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SearchTopBar(
                uiState = SearchUiState.Initial(query = "Attack on Titan"),
                actions = SearchActions(),
                focusRequester = remember { FocusRequester() },
            )
        }
    }
}
