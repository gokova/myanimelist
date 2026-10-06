package com.gokova.myanimelist.feature.search.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.domain.usecase.SearchAnimeUseCase
import com.gokova.myanimelist.feature.search.presentation.SearchActions

private data class SearchInputActions(
    val onSearch: () -> Unit,
    val onClear: () -> Unit,
    val onBack: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
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
        onQueryChange = actions.onQueryChange,
    )

    val currentText = textFieldState.text.toString()
    val canSearch = currentText.trim().length >= SearchAnimeUseCase.MIN_QUERY_LENGTH
    val showValidationHint = currentText.isNotBlank() && !canSearch

    val inputActions =
        rememberSearchInputActions(
            textFieldState = textFieldState,
            latestQuery = latestQuery,
            canSearch = canSearch,
            actions = actions,
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium),
    ) {
        SearchBarContainer(
            textFieldState = textFieldState,
            canSearch = canSearch,
            focusRequester = focusRequester,
            inputActions = inputActions,
        )
        ValidationHint(visible = showValidationHint)
    }
}

@Composable
private fun rememberSearchInputActions(
    textFieldState: TextFieldState,
    latestQuery: String,
    canSearch: Boolean,
    actions: SearchActions,
): SearchInputActions {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    return remember(textFieldState, latestQuery, canSearch, actions) {
        SearchInputActions(
            onSearch = {
                if (canSearch) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val currentText = textFieldState.text.toString()
                    if (currentText != latestQuery) {
                        actions.onQueryChange(currentText)
                    }
                    actions.onSearch()
                }
            },
            onClear = {
                textFieldState.clearText()
                actions.onClearQuery()
            },
            onBack = {
                keyboardController?.hide()
                focusManager.clearFocus()
                actions.onBackClick()
            },
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
        colors =
            SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
        if (textFieldState.text.toString() != query) {
            textFieldState.setTextAndPlaceCursorAtEnd(query)
        }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { newQuery ->
                if (newQuery != latestQuery) {
                    onQueryChange(newQuery)
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputField(
    textFieldState: TextFieldState,
    canSearch: Boolean,
    focusRequester: FocusRequester,
    inputActions: SearchInputActions,
) {
    SearchBarDefaults.InputField(
        state = textFieldState,
        onSearch = { inputActions.onSearch() },
        expanded = false,
        onExpandedChange = {},
        placeholder = {
            Text(
                text = stringResource(R.string.search_placeholder),
                style = MaterialTheme.typography.bodyLarge,
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
                onClearQuery = inputActions.onClear,
                onSearch = inputActions.onSearch,
            )
        },
        modifier =
            Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        textFieldState.edit { placeCursorAtEnd() }
                    }
                },
    )
}

@Composable
private fun ValidationHint(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Text(
            text = stringResource(R.string.search_validation_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier =
                Modifier.padding(
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
    onClearQuery: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (query.isNotEmpty()) {
            IconButton(
                onClick = onClearQuery,
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
            onClick = onSearch,
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
    }
}

@StandardPreviews
@Composable
private fun SearchTopBarPreview() {
    MyAnimeListTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SearchTopBar(
                query = "Frieren",
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
                focusRequester = remember { FocusRequester() },
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
            )
        }
    }
}

@StandardPreviews
@Composable
private fun SearchTopBarEmptyPreview() {
    MyAnimeListTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SearchTopBar(
                query = "",
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
                focusRequester = remember { FocusRequester() },
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
            )
        }
    }
}
