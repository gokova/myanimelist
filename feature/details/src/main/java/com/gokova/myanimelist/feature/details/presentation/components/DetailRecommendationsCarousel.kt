package com.gokova.myanimelist.feature.details.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gokova.myanimelist.core.ui.component.badge.PublicScoreBadge
import com.gokova.myanimelist.core.ui.theme.componentSizes
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.details.R
import com.gokova.myanimelist.feature.details.domain.model.DetailRecommendation

@Composable
fun DetailRecommendationsCarousel(
    recommendations: List<DetailRecommendation>,
    onAnimeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recommendations.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text(
            text = stringResource(R.string.details_recommendations_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(recommendations, key = { it.id }) { item ->
                RecommendationCard(
                    item = item,
                    onClick = { onAnimeClick(item.id) },
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    item: DetailRecommendation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .width(MaterialTheme.componentSizes.carouselPosterWidth)
                .clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
    ) {
        Column {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription =
                    stringResource(
                        R.string.details_poster_content_description,
                        item.title,
                    ),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.componentSizes.carouselPosterHeight)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            RecommendationCardInfo(item = item)
        }
    }
}

@Composable
private fun RecommendationCardInfo(
    item: DetailRecommendation,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(6.dp)) {
        if (item.meanScore != null) {
            PublicScoreBadge(score = item.meanScore)
            Spacer(modifier = Modifier.height(2.dp))
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text =
                pluralStringResource(
                    R.plurals.details_rec_users_count,
                    item.numRecommendations,
                    item.numRecommendations,
                ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
