package com.gokova.myanimelist.feature.search.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.OnStatusCompletedContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusDroppedContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusOnHoldContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusPlanToWatchContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusWatchingContainer
import com.gokova.myanimelist.core.ui.theme.StatusCompletedContainer
import com.gokova.myanimelist.core.ui.theme.StatusDroppedContainer
import com.gokova.myanimelist.core.ui.theme.StatusOnHoldContainer
import com.gokova.myanimelist.core.ui.theme.StatusPlanToWatchContainer
import com.gokova.myanimelist.core.ui.theme.StatusWatchingContainer
import com.gokova.myanimelist.core.ui.theme.componentSizes
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem
import com.gokova.myanimelist.feature.search.presentation.SearchAnimeItemPreviewParameterProvider
import java.util.Locale

private const val MAX_GENRE_TAGS = 3

@Composable
fun SearchAnimeCard(
    anime: SearchAnimeItem,
    onCardClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onCardClick(anime.id) },
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
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
            SearchPosterThumbnail(
                imageUrl = anime.thumbnailUrl,
                title = anime.displayTitle,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
                SearchTitleSection(
                    displayTitle = anime.displayTitle,
                    subtitleTitle = anime.subtitleTitle,
                )

                SearchBadgesRow(
                    matchPercent = anime.tasteMatchPercent,
                    meanScore = anime.meanScore,
                    mediaType = anime.mediaType,
                    numEpisodes = anime.numEpisodes,
                )

                if (anime.userStatus != null) {
                    SearchStatusChip(status = anime.userStatus)
                }

                if (anime.genres.isNotEmpty()) {
                    SearchGenreTags(genres = anime.genres)
                }
            }
        }
    }
}

@Composable
private fun SearchPosterThumbnail(
    imageUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(surfaceVariant) { ColorPainter(surfaceVariant) }

    Box(
        modifier =
            modifier
                .size(
                    width = MaterialTheme.componentSizes.posterSmallWidth,
                    height = MaterialTheme.componentSizes.posterSmallHeight,
                ).clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                placeholder = placeholderPainter,
                error = placeholderPainter,
                contentDescription =
                    stringResource(
                        R.string.search_poster_content_description,
                        title,
                    ),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier =
                    Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun SearchTitleSection(
    displayTitle: String,
    subtitleTitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleMedium,
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
private fun SearchBadgesRow(
    matchPercent: Int?,
    meanScore: Double?,
    mediaType: String?,
    numEpisodes: Int?,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        if (matchPercent != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    text = stringResource(R.string.search_match_percent, matchPercent),
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    modifier =
                        Modifier.padding(
                            horizontal = MaterialTheme.spacing.extraSmall,
                            vertical = 2.dp,
                        ),
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            val scoreText =
                if (meanScore != null && meanScore > 0.0) {
                    stringResource(R.string.search_score_format, meanScore)
                } else {
                    stringResource(R.string.search_score_unrated)
                }
            Text(
                text = scoreText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val formatText = buildFormatText(mediaType, numEpisodes)
        if (formatText.isNotBlank()) {
            Text(
                text = formatText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun buildFormatText(
    mediaType: String?,
    numEpisodes: Int?,
): String {
    val typePart =
        mediaType
            ?.replace('_', ' ')
            ?.trim()
            ?.uppercase(Locale.US)
            .orEmpty()
    val episodesPart =
        when {
            numEpisodes != null && numEpisodes > 0 ->
                pluralStringResource(R.plurals.search_episodes_count, numEpisodes, numEpisodes)
            numEpisodes == 0 &&
                typePart.isNotBlank() &&
                !typePart.equals("MOVIE", ignoreCase = true) ->
                stringResource(R.string.search_episodes_unknown)
            else -> null
        }
    return when {
        typePart.isNotBlank() && episodesPart != null -> "$typePart • $episodesPart"
        typePart.isNotBlank() -> typePart
        episodesPart != null -> episodesPart
        else -> ""
    }
}

@Composable
private fun SearchStatusChip(
    status: String,
    modifier: Modifier = Modifier,
) {
    val (bgColor, fgColor, textRes) =
        when (status.lowercase(Locale.US)) {
            "watching" ->
                Triple(
                    StatusWatchingContainer,
                    OnStatusWatchingContainer,
                    R.string.search_status_watching,
                )
            "completed" ->
                Triple(
                    StatusCompletedContainer,
                    OnStatusCompletedContainer,
                    R.string.search_status_completed,
                )
            "on_hold" ->
                Triple(
                    StatusOnHoldContainer,
                    OnStatusOnHoldContainer,
                    R.string.search_status_on_hold,
                )
            "dropped" ->
                Triple(
                    StatusDroppedContainer,
                    OnStatusDroppedContainer,
                    R.string.search_status_dropped,
                )
            else ->
                Triple(
                    StatusPlanToWatchContainer,
                    OnStatusPlanToWatchContainer,
                    R.string.search_status_plan_to_watch,
                )
        }

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(textRes),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = fgColor,
                ),
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.extraSmall,
                    vertical = 2.dp,
                ),
        )
    }
}

@Composable
private fun SearchGenreTags(
    genres: List<String>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        maxItemsInEachRow = MAX_GENRE_TAGS,
    ) {
        genres.take(MAX_GENRE_TAGS).forEach { genre ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = genre,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier =
                        Modifier.padding(
                            horizontal = MaterialTheme.spacing.small,
                            vertical = 2.dp,
                        ),
                )
            }
        }
    }
}

@StandardPreviews
@Composable
private fun SearchAnimeCardPreview(
    @PreviewParameter(SearchAnimeItemPreviewParameterProvider::class) anime: SearchAnimeItem,
) {
    MyAnimeListTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
        ) {
            SearchAnimeCard(
                anime = anime,
                onCardClick = {},
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
            )
        }
    }
}
