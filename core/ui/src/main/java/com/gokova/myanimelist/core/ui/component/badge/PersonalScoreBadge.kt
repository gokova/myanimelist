package com.gokova.myanimelist.core.ui.component.badge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.R
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing

/**
 * Standard user rating badge used consistently across My List, Details, and Taste bottom sheets.
 * Uses [MaterialTheme.colorScheme.primaryContainer] (soft blue) with a Star icon and bold score.
 */
@Composable
fun PersonalScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
    withLabel: Boolean = false,
) {
    val scoreText =
        if (score > 0) {
            if (withLabel) {
                stringResource(R.string.score_user_with_label, score)
            } else {
                stringResource(R.string.score_user_format, score)
            }
        } else {
            stringResource(R.string.score_unrated)
        }

    PersonalScoreBadgeLayout(
        scoreText = scoreText,
        modifier = modifier,
        withLabel = withLabel,
    )
}

/**
 * Decimal user rating badge for category or taste averages.
 */
@Composable
fun PersonalScoreBadge(
    score: Double?,
    modifier: Modifier = Modifier,
    withLabel: Boolean = false,
) {
    val scoreText =
        if (score != null && score > 0.0) {
            stringResource(R.string.score_format_compact, score)
        } else {
            stringResource(R.string.score_unrated)
        }

    PersonalScoreBadgeLayout(
        scoreText = scoreText,
        modifier = modifier,
        withLabel = withLabel,
    )
}

@Composable
private fun PersonalScoreBadgeLayout(
    scoreText: String,
    modifier: Modifier = Modifier,
    withLabel: Boolean = false,
) {
    val cd = stringResource(R.string.score_user_content_description, scoreText)

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier =
            modifier.semantics(mergeDescendants = true) {
                this.contentDescription = cd
            },
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.badgePaddingHorizontal,
                    vertical = MaterialTheme.spacing.badgePaddingVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
        ) {
            if (!withLabel) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(MaterialTheme.spacing.iconSmall),
                )
            }
            Text(
                text = scoreText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@StandardPreviews
@Composable
private fun PersonalScoreBadgePreview() {
    MyAnimeListTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            PersonalScoreBadge(score = 9)
            PersonalScoreBadge(score = 9, withLabel = true)
            PersonalScoreBadge(score = 8.5)
            PersonalScoreBadge(score = 0)
        }
    }
}
