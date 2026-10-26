package com.gokova.myanimelist.core.ui.component.badge

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.R
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing

/**
 * Standard badge displaying franchise relations (e.g. "Sequel: Code Geass R1").
 * Uses [MaterialTheme.colorScheme.tertiaryContainer] with [MaterialTheme.shapes.extraSmall].
 */
@Composable
fun RelationBadge(
    relationType: String,
    parentTitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            text = stringResource(R.string.relation_format, relationType, parentTitle),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.micro,
                ),
        )
    }
}

@StandardPreviews
@Composable
private fun RelationBadgePreview() {
    MyAnimeListTheme {
        RelationBadge(
            relationType = "Sequel",
            parentTitle = "Attack on Titan",
            modifier = Modifier.padding(16.dp),
        )
    }
}
