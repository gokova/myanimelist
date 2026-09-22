package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.OnStatusCompletedContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusPlanToWatchContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusWatchingContainer
import com.gokova.myanimelist.core.ui.theme.StatusCompletedContainer
import com.gokova.myanimelist.core.ui.theme.StatusPlanToWatchContainer
import com.gokova.myanimelist.core.ui.theme.StatusWatchingContainer
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.recommendation.R
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime

@Composable
fun NewSeasonCard(
    anime: NewSeasonAnime,
    modifier: Modifier = Modifier,
    onPosterClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            PosterThumbnail(
                imageUrl = anime.thumbnailUrl,
                title = anime.displayTitle,
                onPosterClick = onPosterClick,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
                TitleSection(
                    displayTitle = anime.displayTitle,
                    subtitleTitle = anime.subtitleTitle,
                )

                RelationBadge(
                    relationTypeFormatted = anime.relationTypeFormatted,
                    parentTitle = anime.parentTitle,
                )

                StatsRow(
                    score = anime.score,
                    totalEpisodes = anime.totalEpisodes,
                )

                MetadataChips(
                    mediaType = anime.mediaType,
                    releaseSeason = anime.releaseSeason,
                    airingStatus = anime.airingStatus,
                )
            }
        }
    }
}

@Composable
private fun PosterThumbnail(
    imageUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    onPosterClick: (() -> Unit)? = null,
) {
    val clickableModifier =
        if (onPosterClick != null) {
            Modifier.clickable(
                role = Role.Button,
                onClick = onPosterClick,
            )
        } else {
            Modifier
        }
    val contentDesc =
        if (onPosterClick != null) {
            stringResource(R.string.recommendation_zoom_poster_description, title)
        } else {
            title
        }

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(surfaceVariant) { ColorPainter(surfaceVariant) }
    val backgroundModifier =
        if (imageUrl.isNullOrBlank()) {
            Modifier.background(surfaceVariant)
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .width(80.dp)
                .height(120.dp)
                .clip(MaterialTheme.shapes.small)
                .then(backgroundModifier)
                .semantics { this.contentDescription = contentDesc }
                .then(clickableModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
        } else {
            Text(
                text = title.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TitleSection(
    displayTitle: String,
    subtitleTitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitleTitle != null) {
            Text(
                text = subtitleTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RelationBadge(
    relationTypeFormatted: String,
    parentTitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            text =
                stringResource(
                    R.string.new_seasons_relation_format,
                    relationTypeFormatted,
                    parentTitle,
                ),
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

@Composable
private fun StatsRow(
    score: Double?,
    totalEpisodes: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreBadge(score = score)
        EpisodeBadge(totalEpisodes = totalEpisodes)
    }
}

@Composable
private fun ScoreBadge(
    score: Double?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.micro,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    if (score != null && score > 0.0) {
                        stringResource(R.string.recommendation_score_format, score)
                    } else {
                        stringResource(R.string.recommendation_score_unrated)
                    },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun EpisodeBadge(
    totalEpisodes: Int?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text =
                if (totalEpisodes != null && totalEpisodes > 0) {
                    stringResource(R.string.recommendation_episodes_format, totalEpisodes)
                } else {
                    stringResource(R.string.recommendation_episodes_unknown)
                },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.micro,
                ),
        )
    }
}

@Composable
private fun MetadataChips(
    mediaType: String?,
    releaseSeason: String?,
    airingStatus: String?,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
    ) {
        listOfNotNull(mediaType, releaseSeason).forEach { chipText ->
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Text(
                    text = chipText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.padding(
                            horizontal = MaterialTheme.spacing.small,
                            vertical = MaterialTheme.spacing.micro,
                        ),
                )
            }
        }
        if (!airingStatus.isNullOrBlank()) {
            AiringStatusChip(airingStatus = airingStatus)
        }
    }
}

@Composable
private fun AiringStatusChip(
    airingStatus: String,
    modifier: Modifier = Modifier,
) {
    val (containerColor, contentColor, labelRes) =
        when (airingStatus.lowercase()) {
            "currently_airing" ->
                Triple(
                    StatusWatchingContainer,
                    OnStatusWatchingContainer,
                    R.string.new_seasons_status_currently_airing,
                )
            "finished_airing" ->
                Triple(
                    StatusCompletedContainer,
                    OnStatusCompletedContainer,
                    R.string.new_seasons_status_finished_airing,
                )
            else ->
                Triple(
                    StatusPlanToWatchContainer,
                    OnStatusPlanToWatchContainer,
                    R.string.new_seasons_status_not_yet_aired,
                )
        }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.micro,
                ),
        )
    }
}

class NewSeasonAnimePreviewParameterProvider : PreviewParameterProvider<NewSeasonAnime> {
    override val values: Sequence<NewSeasonAnime> =
        sequenceOf(
            NewSeasonAnime(
                animeId = 30230L,
                displayTitle = "Diamond no Ace: Second Season",
                subtitleTitle = "Daiya no Ace: Second Season",
                thumbnailUrl = null,
                largeImageUrl = null,
                mediaType = "TV",
                releaseSeason = "2015 Spring",
                airingStatus = "finished_airing",
                score = 8.42,
                totalEpisodes = 51,
                parentAnimeId = 18689L,
                parentTitle = "Diamond no Ace",
                relationType = "sequel",
                relationTypeFormatted = "Sequel",
                startSeasonYear = 2015,
                startSeasonSeason = "spring",
            ),
        )

    override fun getDisplayName(index: Int): String = "Diamond no Ace S2"
}

@StandardPreviews
@Composable
private fun NewSeasonCardPreview(
    @PreviewParameter(NewSeasonAnimePreviewParameterProvider::class) anime: NewSeasonAnime,
) {
    MyAnimeListTheme {
        NewSeasonCard(anime = anime)
    }
}
