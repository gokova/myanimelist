package com.gokova.myanimelist.feature.details.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.details.R

@Composable
fun AnimeDetailsTaxonomy(
    genres: List<String>,
    themes: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        if (genres.isNotEmpty()) {
            TaxonomySection(
                title = stringResource(R.string.details_genres_title),
                tags = genres,
                isGenre = true,
            )
        }

        if (themes.isNotEmpty()) {
            TaxonomySection(
                title = stringResource(R.string.details_themes_title),
                tags = themes,
                isGenre = false,
            )
        }
    }
}

@Composable
private fun TaxonomySection(
    title: String,
    tags: List<String>,
    isGenre: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            tags.forEach { tag ->
                TaxonomyChip(
                    text = tag,
                    isGenre = isGenre,
                )
            }
        }
    }
}

@Composable
private fun TaxonomyChip(
    text: String,
    isGenre: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (isGenre) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        }
    val contentColor =
        if (isGenre) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
