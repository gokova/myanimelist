package com.gokova.myanimelist.core.ui.component.badge

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Standard badge indicating taste compatibility percentage across Recommendation and Search cards.
 * Uses [MaterialTheme.colorScheme.primaryContainer] (soft blue) with bold percent text.
 */
@Composable
fun TasteMatchBadge(
    matchPercent: Int,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(R.string.taste_match_format, matchPercent)
    val cd = stringResource(R.string.taste_match_content_description, matchPercent)

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier =
            modifier.semantics(mergeDescendants = true) {
                this.contentDescription = cd
            },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            softWrap = false,
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.badgePaddingHorizontal,
                    vertical = MaterialTheme.spacing.badgePaddingVertical,
                ),
        )
    }
}

@StandardPreviews
@Composable
private fun TasteMatchBadgePreview() {
    MyAnimeListTheme {
        TasteMatchBadge(
            matchPercent = 94,
            modifier = Modifier.padding(16.dp),
        )
    }
}
