package com.gokova.myanimelist.feature.search.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.domain.usecase.AiSearchAnimeUseCase
import com.gokova.myanimelist.feature.search.presentation.AiSearchActions

@Composable
fun AiSearchPromptCard(
    prompt: String,
    actions: AiSearchActions,
    onBackToStandard: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val canSubmit =
        prompt.trim().length >= AiSearchAnimeUseCase.MIN_PROMPT_LENGTH && !isLoading
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
        ) {
            AiPromptCardHeader(onBack = onBackToStandard, isLoading = isLoading)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            OutlinedTextField(
                value = prompt,
                onValueChange = {
                    if (it.length <= AiSearchAnimeUseCase.MAX_PROMPT_LENGTH) {
                        actions.onPromptChange(it)
                    }
                },
                enabled = !isLoading,
                placeholder = {
                    Text(
                        text = stringResource(R.string.ai_search_prompt_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Search,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onSearch = {
                            if (canSubmit) {
                                actions.onSubmitPrompt()
                            }
                        },
                    ),
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            AiPromptCardBottomRow(
                prompt = prompt,
                actions = actions,
                isLoading = isLoading,
            )
        }
    }
}

@Composable
private fun AiPromptCardHeader(
    onBack: () -> Unit,
    isLoading: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                enabled = !isLoading,
                modifier = Modifier.size(MaterialTheme.spacing.minTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription =
                        stringResource(R.string.search_back_content_description),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(R.string.ai_search_initial_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AiPromptCardBottomRow(
    prompt: String,
    actions: AiSearchActions,
    isLoading: Boolean,
) {
    val canSubmit =
        prompt.trim().length >= AiSearchAnimeUseCase.MIN_PROMPT_LENGTH && !isLoading
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text =
                    stringResource(
                        R.string.ai_search_char_counter,
                        prompt.length,
                        AiSearchAnimeUseCase.MAX_PROMPT_LENGTH,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedVisibility(
                visible = prompt.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(
                    onClick = actions.onClearPrompt,
                    enabled = !isLoading,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription =
                            stringResource(R.string.search_clear_content_description),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Button(
            onClick = actions.onSubmitPrompt,
            enabled = canSubmit,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
            Text(stringResource(R.string.ai_search_submit_button))
        }
    }
}
