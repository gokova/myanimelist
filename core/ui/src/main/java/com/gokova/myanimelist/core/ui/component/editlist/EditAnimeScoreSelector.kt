package com.gokova.myanimelist.core.ui.component.editlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.R

private const val MAX_SCORE = 10
private val SCORE_OPTIONS = listOf(0) + (MAX_SCORE downTo 1).toList()
private val SCORE_RATINGS_MAP =
    mapOf(
        0 to R.string.score_not_rated,
        10 to R.string.score_10,
        9 to R.string.score_9,
        8 to R.string.score_8,
        7 to R.string.score_7,
        6 to R.string.score_6,
        5 to R.string.score_5,
        4 to R.string.score_4,
        3 to R.string.score_3,
        2 to R.string.score_2,
        1 to R.string.score_1,
    )

@Composable
fun EditAnimeScoreSelector(
    score: Int,
    onScoreChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val currentLabelRes = SCORE_RATINGS_MAP[score] ?: R.string.score_not_rated

    Column(modifier = modifier.fillMaxWidth()) {
        ScoreHeader(labelRes = currentLabelRes)
        Spacer(modifier = Modifier.height(8.dp))
        ScoreChipRow(
            score = score,
            onScoreChange = onScoreChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun ScoreHeader(labelRes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.edit_list_status_section_score),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ScoreChipRow(
    score: Int,
    onScoreChange: (Int) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SCORE_OPTIONS.forEach { scoreValue ->
            val labelRes = SCORE_RATINGS_MAP[scoreValue] ?: R.string.score_not_rated
            ScoreChip(
                scoreValue = scoreValue,
                labelRes = labelRes,
                isSelected = score == scoreValue,
                enabled = enabled,
                onClick = { onScoreChange(scoreValue) },
            )
        }
    }
}

@Composable
private fun ScoreChip(
    scoreValue: Int,
    labelRes: Int,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val displayText = if (scoreValue == 0) "-" else scoreValue.toString()
    val ratingLabel = stringResource(labelRes)
    val a11yDescription =
        if (scoreValue == 0) {
            ratingLabel
        } else {
            stringResource(R.string.score_option_a11y_format, scoreValue, ratingLabel)
        }

    val containerColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    val contentColor =
        if (!enabled) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        } else if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val borderColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        }

    Surface(
        selected = isSelected,
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier =
            Modifier
                .size(48.dp)
                .semantics {
                    role = Role.RadioButton
                    contentDescription = a11yDescription
                },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(2.dp),
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
            )
        }
    }
}
