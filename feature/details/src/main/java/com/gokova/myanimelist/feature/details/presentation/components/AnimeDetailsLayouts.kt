package com.gokova.myanimelist.feature.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.presentation.AnimeDetailsActions

private const val WIDE_PRIMARY_WEIGHT = 0.62f
private const val WIDE_DISCOVERY_WEIGHT = 0.38f

@Composable
fun WideAnimeDetailsLayout(
    details: AnimeDetails,
    isAddingToList: Boolean,
    actions: AnimeDetailsActions,
    onPosterClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(WIDE_PRIMARY_WEIGHT),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            ) {
                AnimeDetailsHeader(
                    details = details,
                    isAddingToList = isAddingToList,
                    actions = actions,
                    onPosterClick = onPosterClick,
                )
                AnimeDetailsSynopsis(synopsis = details.synopsis)
            }

            Column(
                modifier = Modifier.weight(WIDE_DISCOVERY_WEIGHT),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            ) {
                AnimeDetailsMetadata(details = details)
                AnimeDetailsTaxonomy(genres = details.genres, themes = details.themes)
            }
        }

        RelatedAnimeCarousel(
            relatedAnime = details.relatedAnime,
            onAnimeClick = actions.onAnimeClick,
        )
        DetailRecommendationsCarousel(
            recommendations = details.recommendations,
            onAnimeClick = actions.onAnimeClick,
        )
    }
}

@Composable
fun CompactAnimeDetailsLayout(
    details: AnimeDetails,
    isAddingToList: Boolean,
    actions: AnimeDetailsActions,
    onPosterClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)) {
        AnimeDetailsHeader(
            details = details,
            isAddingToList = isAddingToList,
            actions = actions,
            onPosterClick = onPosterClick,
        )
        AnimeDetailsSynopsis(synopsis = details.synopsis)
        AnimeDetailsMetadata(details = details)
        AnimeDetailsTaxonomy(genres = details.genres, themes = details.themes)
        RelatedAnimeCarousel(
            relatedAnime = details.relatedAnime,
            onAnimeClick = actions.onAnimeClick,
        )
        DetailRecommendationsCarousel(
            recommendations = details.recommendations,
            onAnimeClick = actions.onAnimeClick,
        )
    }
}
