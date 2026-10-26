package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.gokova.myanimelist.core.ui.component.badge.PublicScoreBadge
import com.gokova.myanimelist.core.ui.component.badge.RelationBadge
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.OnStatusCompletedContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusPlanToWatchContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusWatchingContainer
import com.gokova.myanimelist.core.ui.theme.StatusCompletedContainer
import com.gokova.myanimelist.core.ui.theme.StatusPlanToWatchContainer
import com.gokova.myanimelist.core.ui.theme.StatusWatchingContainer
import com.gokova.myanimelist.core.ui.theme.componentSizes
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.recommendation.R
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewSeasonCard(
    anime: NewSeasonAnime,
    modifier: Modifier = Modifier,
    actions: RecommendationCardActions = RecommendationCardActions(),
) {
    val cardClickableModifier =
        if (actions.onCardClick != null || actions.onCardLongClick != null) {
            Modifier.combinedClickable(
                role = Role.Button,
                onClick = { actions.onCardClick?.invoke() },
                onLongClick = actions.onCardLongClick,
            )
        } else {
            Modifier
        }

    Card(
        modifier = modifier.fillMaxWidth().then(cardClickableModifier),
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
                onPosterClick = actions.onPosterClick,
            )

            NewSeasonCardContent(
                anime = anime,
                onMoreClick = actions.onMoreClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NewSeasonCardContent(
    anime: NewSeasonAnime,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        TitleSection(
            displayTitle = anime.displayTitle,
            subtitleTitle = anime.subtitleTitle,
            onMoreClick = onMoreClick,
        )

        RelationBadge(
            relationType = anime.relationTypeFormatted,
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
                .width(MaterialTheme.componentSizes.posterSmallWidth)
                .height(MaterialTheme.componentSizes.posterSmallHeight)
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.componentSizes.posterSmallHeight),
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
    onMoreClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        if (onMoreClick != null) {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription =
                        stringResource(R.string.recommendation_action_more_options, displayTitle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
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
        PublicScoreBadge(score = score)
        EpisodeBadge(totalEpisodes = totalEpisodes)
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
