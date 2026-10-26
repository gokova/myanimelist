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
 * Standard public rating badge used consistently across discovery, list, search, and detail screens.
 * Uses [MaterialTheme.colorScheme.tertiaryContainer] (soft purple) with a Star icon.
 */
@Composable
fun PublicScoreBadge(
    score: Double?,
    modifier: Modifier = Modifier,
    useTwoDecimals: Boolean = false,
) {
    val scoreText =
        if (score != null && score > 0.0) {
            if (useTwoDecimals) {
                stringResource(R.string.score_format_details, score)
            } else {
                stringResource(R.string.score_format_compact, score)
            }
        } else {
            stringResource(R.string.score_unrated)
        }

    val cd = stringResource(R.string.score_public_content_description, scoreText)

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.tertiaryContainer,
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
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(MaterialTheme.spacing.iconSmall),
            )
            Text(
                text = scoreText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@StandardPreviews
@Composable
private fun PublicScoreBadgePreview() {
    MyAnimeListTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            PublicScoreBadge(score = 8.76)
            PublicScoreBadge(score = 8.76, useTwoDecimals = true)
            PublicScoreBadge(score = null)
        }
    }
}
