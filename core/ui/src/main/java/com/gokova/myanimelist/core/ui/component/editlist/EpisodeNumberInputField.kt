package com.gokova.myanimelist.core.ui.component.editlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.R
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun EpisodeNumberInputField(
    episodesWatched: Int,
    totalEpisodes: Int?,
    onEpisodesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }
    val textFieldState = rememberTextFieldState(episodesWatched.toString())
    val inputTransformation = remember(totalEpisodes) { EpisodeInputTransformation(totalEpisodes) }
    val latestEpisodesWatched by rememberUpdatedState(episodesWatched)
    val latestOnEpisodesChange by rememberUpdatedState(onEpisodesChange)

    SyncEpisodesWithState(
        episodesWatched = episodesWatched,
        isFocused = isFocused,
        textFieldState = textFieldState,
        onEpisodesChange = latestOnEpisodesChange,
    )

    val onReset = {
        textFieldState.edit {
            replace(0, length, "0")
            placeCursorAtEnd()
        }
        if (latestEpisodesWatched != 0) {
            latestOnEpisodesChange(0)
        }
    }

    EpisodeInputTextField(
        viewState =
            EpisodeInputViewState(
                state = textFieldState,
                isFocused = isFocused,
                enabled = enabled,
                inputTransformation = inputTransformation,
            ),
        onReset = onReset,
        onFocusChanged = { focused ->
            val wasFocused = isFocused
            isFocused = focused
            if (focused) {
                textFieldState.edit { selection = TextRange(0, length) }
            } else if (wasFocused && textFieldState.text.isEmpty()) {
                onReset()
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun SyncEpisodesWithState(
    episodesWatched: Int,
    isFocused: Boolean,
    textFieldState: TextFieldState,
    onEpisodesChange: (Int) -> Unit,
) {
    LaunchedEffect(episodesWatched) {
        val currentText = textFieldState.text.toString()
        if (!isFocused || currentText.toIntOrNull() != episodesWatched) {
            val newText = episodesWatched.toString()
            if (currentText != newText) {
                textFieldState.edit {
                    replace(0, length, newText)
                    if (isFocused) {
                        selection = TextRange(0, newText.length)
                    } else {
                        placeCursorAtEnd()
                    }
                }
            }
        }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { text ->
                if (text.isNotEmpty()) {
                    val parsed = text.toIntOrNull()
                    if (parsed != null && parsed != episodesWatched) {
                        onEpisodesChange(parsed)
                    }
                }
            }
    }
}

private data class EpisodeInputViewState(
    val state: TextFieldState,
    val isFocused: Boolean,
    val enabled: Boolean,
    val inputTransformation: InputTransformation,
)

@Composable
private fun EpisodeInputTextField(
    viewState: EpisodeInputViewState,
    onReset: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val inputDescription =
        stringResource(R.string.edit_list_status_episodes_input_content_description)

    BasicTextField(
        state = viewState.state,
        enabled = viewState.enabled,
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = viewState.inputTransformation,
        textStyle =
            MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = getEpisodeInputTextColor(viewState.enabled),
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
        onKeyboardAction = {
            if (viewState.state.text.isEmpty()) onReset()
            focusManager.clearFocus()
        },
        modifier =
            modifier
                .width(64.dp)
                .heightIn(min = 48.dp)
                .semantics { contentDescription = inputDescription }
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                },
        decorator = { innerTextField ->
            EpisodeInputDecoration(
                isFocused = viewState.isFocused,
                enabled = viewState.enabled,
                innerTextField = innerTextField,
            )
        },
    )
}

internal class EpisodeInputTransformation(
    private val totalEpisodes: Int?,
) : InputTransformation {
    override val keyboardOptions: KeyboardOptions =
        KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        )

    override fun TextFieldBuffer.transformInput() {
        val newText = asCharSequence().toString()
        if (newText.isEmpty()) return
        val (sanitized, _) = sanitizeEpisodeInput(newText, totalEpisodes)
        if (sanitized != newText) {
            if (sanitized.isEmpty() && newText.isNotEmpty()) {
                revertAllChanges()
            } else {
                replace(0, length, sanitized)
            }
        }
    }
}

internal fun sanitizeEpisodeInput(
    text: String,
    totalEpisodes: Int?,
    currentText: String = "",
): Pair<String, Int?> {
    val digitsOnly = text.filter { it.isDigit() }
    if (digitsOnly.isEmpty()) {
        return "" to null
    }
    val trimmed = digitsOnly.trimStart('0')
    val parsed = if (trimmed.isEmpty()) 0 else trimmed.toIntOrNull()
    val isKnownTotal = totalEpisodes != null && totalEpisodes > 0

    return if (parsed != null) {
        val clamped = if (isKnownTotal && parsed > totalEpisodes) totalEpisodes else parsed
        clamped.toString() to clamped
    } else if (isKnownTotal) {
        totalEpisodes.toString() to totalEpisodes
    } else {
        currentText to null
    }
}

internal fun sanitizeEpisodeInput(
    newValue: TextFieldValue,
    totalEpisodes: Int?,
    currentValue: TextFieldValue = TextFieldValue(),
): Pair<TextFieldValue, Int?> {
    val (sanitizedText, parsed) =
        sanitizeEpisodeInput(
            newValue.text,
            totalEpisodes,
            currentValue.text,
        )
    val selection =
        if (sanitizedText != newValue.text) {
            TextRange(sanitizedText.length)
        } else {
            newValue.selection
        }
    return newValue.copy(text = sanitizedText, selection = selection) to parsed
}

@Composable
private fun getEpisodeInputTextColor(enabled: Boolean): Color =
    if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

@Composable
private fun EpisodeInputDecoration(
    isFocused: Boolean,
    enabled: Boolean,
    innerTextField: @Composable () -> Unit,
) {
    val borderColor =
        when {
            !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
            isFocused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        }
    val borderWidth = if (isFocused) 2.dp else 1.dp

    Box(contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(borderWidth, borderColor),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(40.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                innerTextField()
            }
        }
    }
}

@StandardPreviews
@Composable
private fun EpisodeNumberInputFieldPreview() {
    MyAnimeListTheme {
        Surface {
            EpisodeNumberInputField(
                episodesWatched = 51,
                totalEpisodes = 51,
                onEpisodesChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
