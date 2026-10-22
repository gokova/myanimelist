package com.gokova.myanimelist.core.ui.component.editlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun EpisodeNumberInputField(
    episodesWatched: Int,
    totalEpisodes: Int?,
    onEpisodesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = episodesWatched.toString(),
                selection = TextRange(episodesWatched.toString().length),
            ),
        )
    }

    LaunchedEffect(episodesWatched) {
        if (!isFocused || textFieldValue.text.toIntOrNull() != episodesWatched) {
            textFieldValue = syncTextFieldValue(textFieldValue, episodesWatched, isFocused)
        }
    }

    EpisodeInputTextField(
        state =
            EpisodeInputViewState(
                value = textFieldValue,
                isFocused = isFocused,
                enabled = enabled,
            ),
        onValueChange = { newValue ->
            val (sanitized, parsed) =
                sanitizeEpisodeInput(
                    newValue = newValue,
                    totalEpisodes = totalEpisodes,
                    currentValue = textFieldValue,
                )
            textFieldValue = sanitized
            if (parsed != null) {
                onEpisodesChange(parsed)
            }
        },
        onReset = {
            textFieldValue = TextFieldValue("0", selection = TextRange(1))
            onEpisodesChange(0)
        },
        onFocusChanged = { focused ->
            isFocused = focused
            if (focused) {
                val text = textFieldValue.text
                textFieldValue = textFieldValue.copy(selection = TextRange(0, text.length))
            }
        },
        modifier = modifier,
    )
}

private data class EpisodeInputViewState(
    val value: TextFieldValue,
    val isFocused: Boolean,
    val enabled: Boolean,
)

@Composable
private fun EpisodeInputTextField(
    state: EpisodeInputViewState,
    onValueChange: (TextFieldValue) -> Unit,
    onReset: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val inputDescription =
        stringResource(R.string.edit_list_status_episodes_input_content_description)

    BasicTextField(
        value = state.value,
        onValueChange = { if (state.enabled) onValueChange(it) },
        enabled = state.enabled,
        singleLine = true,
        textStyle =
            MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = getEpisodeInputTextColor(state.enabled),
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    if (state.value.text.isEmpty()) onReset()
                    focusManager.clearFocus()
                },
            ),
        modifier =
            modifier
                .width(64.dp)
                .height(40.dp)
                .semantics { contentDescription = inputDescription }
                .onFocusChanged { focusState ->
                    val wasFocused = state.isFocused
                    onFocusChanged(focusState.isFocused)
                    if (wasFocused && !focusState.isFocused && state.value.text.isEmpty()) {
                        onReset()
                    }
                },
        decorationBox = { innerTextField ->
            EpisodeInputDecoration(
                isFocused = state.isFocused,
                enabled = state.enabled,
                innerTextField = innerTextField,
            )
        },
    )
}

private fun syncTextFieldValue(
    current: TextFieldValue,
    episodesWatched: Int,
    isFocused: Boolean,
): TextFieldValue {
    val newText = episodesWatched.toString()
    val selection = if (isFocused) TextRange(0, newText.length) else TextRange(newText.length)
    return current.copy(text = newText, selection = selection)
}

internal fun sanitizeEpisodeInput(
    newValue: TextFieldValue,
    totalEpisodes: Int?,
    currentValue: TextFieldValue = TextFieldValue(),
): Pair<TextFieldValue, Int?> {
    val digitsOnly = newValue.text.filter { it.isDigit() }
    if (digitsOnly.isEmpty()) {
        return newValue.copy(text = "") to null
    }
    val trimmed = digitsOnly.trimStart('0')
    val parsed = if (trimmed.isEmpty()) 0 else trimmed.toIntOrNull()
    val isKnownTotal = totalEpisodes != null && totalEpisodes > 0

    return if (parsed != null) {
        val clamped = if (isKnownTotal && parsed > totalEpisodes) totalEpisodes else parsed
        val clampedText = clamped.toString()
        val selection =
            if (clampedText != newValue.text) {
                TextRange(clampedText.length)
            } else {
                newValue.selection
            }
        newValue.copy(text = clampedText, selection = selection) to clamped
    } else if (isKnownTotal) {
        val clampedText = totalEpisodes.toString()
        newValue.copy(text = clampedText, selection = TextRange(clampedText.length)) to
            totalEpisodes
    } else {
        currentValue to null
    }
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

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier.height(40.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            innerTextField()
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
