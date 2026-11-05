@file:Suppress("TooManyFunctions")

package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.recommendation.R
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationLabConfig
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationPreset
import com.gokova.myanimelist.feature.recommendation.domain.model.TimeCommitmentFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationLabBottomSheet(
    config: RecommendationLabConfig,
    matchCount: Int,
    actions: RecommendationLabActions,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        SimulatedLabOverlay(
            config = config,
            matchCount = matchCount,
            actions = actions,
            modifier = modifier,
        )
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = actions.onDismissRequest,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = modifier,
        ) {
            RecommendationLabContent(
                config = config,
                matchCount = matchCount,
                actions = actions,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimulatedLabOverlay(
    config: RecommendationLabConfig,
    matchCount: Int,
    actions: RecommendationLabActions,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BottomSheetDefaults.DragHandle()
                RecommendationLabContent(
                    config = config,
                    matchCount = matchCount,
                    actions = actions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RecommendationLabContent(
    config: RecommendationLabConfig,
    matchCount: Int,
    actions: RecommendationLabActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            LabHeader(
                isDefault = config.isDefault,
                onReset = actions.onReset,
            )

            LabPresetSelector(
                currentPreset = config.preset,
                onPresetSelected = { actions.onConfigChange(it.toConfig()) },
            )

            LabSliders(
                config = config,
                onConfigChange = actions.onConfigChange,
            )

            LabTimeCommitmentSelector(
                currentFilter = config.timeCommitment,
                onFilterSelected = {
                    actions.onConfigChange(
                        config.copy(preset = RecommendationPreset.CUSTOM, timeCommitment = it),
                    )
                },
            )

            LabMinScoreSelector(
                currentScore = config.minCommunityScore,
                onScoreSelected = {
                    actions.onConfigChange(
                        config.copy(preset = RecommendationPreset.CUSTOM, minCommunityScore = it),
                    )
                },
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        }

        LabFooter(
            matchCount = matchCount,
            onApply = actions.onApply,
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.large),
        )
    }
}

@Composable
private fun LabHeader(
    isDefault: Boolean,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.recommendation_lab_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(
            onClick = onReset,
            enabled = !isDefault,
        ) {
            Text(text = stringResource(R.string.recommendation_lab_reset))
        }
    }
}

@Composable
private fun LabPresetSelector(
    currentPreset: RecommendationPreset,
    onPresetSelected: (RecommendationPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets =
        remember {
            listOf(
                RecommendationPreset.COMFORT_ZONE,
                RecommendationPreset.BALANCED,
                RecommendationPreset.SERENDIPITY,
                RecommendationPreset.HIDDEN_GEMS,
                RecommendationPreset.WEEKEND_BINGE,
                RecommendationPreset.LONG_COMMITMENT,
            )
        }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        Text(
            text = stringResource(R.string.recommendation_lab_preset_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            presets.forEach { preset ->
                FilterChip(
                    selected = currentPreset == preset,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(text = stringResource(preset.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun LabSliders(
    config: RecommendationLabConfig,
    onConfigChange: (RecommendationLabConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        SingleSlider(
            title = stringResource(R.string.recommendation_lab_familiarity_title),
            minLabel = stringResource(R.string.recommendation_lab_familiarity_familiar),
            maxLabel = stringResource(R.string.recommendation_lab_familiarity_surprising),
            value = config.familiarity,
            onValueChange = {
                onConfigChange(
                    config.copy(preset = RecommendationPreset.CUSTOM, familiarity = it),
                )
            },
        )

        SingleSlider(
            title = stringResource(R.string.recommendation_lab_popularity_title),
            minLabel = stringResource(R.string.recommendation_lab_popularity_mainstream),
            maxLabel = stringResource(R.string.recommendation_lab_popularity_niche),
            value = config.popularity,
            onValueChange = {
                onConfigChange(
                    config.copy(preset = RecommendationPreset.CUSTOM, popularity = it),
                )
            },
        )
    }
}

@Composable
private fun SingleSlider(
    title: String,
    minLabel: String,
    maxLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = minLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = maxLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LabTimeCommitmentSelector(
    currentFilter: TimeCommitmentFilter,
    onFilterSelected: (TimeCommitmentFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        Text(
            text = stringResource(R.string.recommendation_lab_time_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            TimeCommitmentFilter.entries.forEach { filter ->
                FilterChip(
                    selected = currentFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(text = stringResource(filter.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun LabMinScoreSelector(
    currentScore: Double?,
    onScoreSelected: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scoreOptions = remember { listOf(null, 6.5, 7.0, 7.5, 8.0) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        Text(
            text = stringResource(R.string.recommendation_lab_min_score_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            scoreOptions.forEach { score ->
                val label =
                    if (score == null) {
                        stringResource(R.string.recommendation_lab_min_score_any)
                    } else {
                        stringResource(R.string.recommendation_lab_min_score_format, score)
                    }
                FilterChip(
                    selected = currentScore == score,
                    onClick = { onScoreSelected(score) },
                    label = { Text(text = label) },
                )
            }
        }
    }
}

@Composable
private fun LabFooter(
    matchCount: Int,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text =
                pluralStringResource(
                    R.plurals.recommendation_lab_matches_count,
                    matchCount,
                    matchCount,
                ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(R.string.recommendation_lab_apply),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private val RecommendationPreset.labelRes: Int
    get() =
        when (this) {
            RecommendationPreset.COMFORT_ZONE -> R.string.recommendation_lab_preset_comfort_zone
            RecommendationPreset.BALANCED -> R.string.recommendation_lab_preset_balanced
            RecommendationPreset.SERENDIPITY -> R.string.recommendation_lab_preset_serendipity
            RecommendationPreset.HIDDEN_GEMS -> R.string.recommendation_lab_preset_hidden_gems
            RecommendationPreset.WEEKEND_BINGE -> R.string.recommendation_lab_preset_weekend_binge
            RecommendationPreset.LONG_COMMITMENT ->
                R.string.recommendation_lab_preset_long_commitment
            RecommendationPreset.CUSTOM -> R.string.recommendation_lab_preset_custom
        }

private val TimeCommitmentFilter.labelRes: Int
    get() =
        when (this) {
            TimeCommitmentFilter.ANY -> R.string.recommendation_lab_time_any
            TimeCommitmentFilter.SHORT -> R.string.recommendation_lab_time_short
            TimeCommitmentFilter.MEDIUM -> R.string.recommendation_lab_time_medium
            TimeCommitmentFilter.LONG -> R.string.recommendation_lab_time_long
        }

@StandardPreviews
@Composable
private fun RecommendationLabBottomSheetPreview() {
    MyAnimeListTheme {
        SimulatedLabOverlay(
            config = RecommendationLabConfig(),
            matchCount = 142,
            actions = RecommendationLabActions(),
        )
    }
}
