package com.gokova.myanimelist.feature.mylist.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.mylist.R
import com.gokova.myanimelist.feature.mylist.domain.model.AiringStatus
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime

@Composable
fun AnimeCard(
    anime: UserAnime,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            PosterThumbnail(
                imageUrl = anime.imageUrl,
                title = anime.displayTitle,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
                TitleSection(
                    displayTitle = anime.displayTitle,
                    subtitleTitle = anime.subtitleTitle,
                )

                MetadataRow(
                    mediaType = anime.mediaType,
                    releaseSeason = anime.releaseSeason,
                    airingStatus = anime.airingStatus,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                ProgressSection(
                    watchedEpisodes = anime.watchedEpisodes,
                    totalEpisodes = anime.totalEpisodes,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.micro))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScoreBadge(score = anime.userScore)
                    AnimeStatusChip(status = anime.userStatus)
                }
            }
        }
    }
}

@Composable
private fun PosterThumbnail(
    imageUrl: String?,
    title: String,
) {
    Box(
        modifier =
            Modifier
                .width(80.dp)
                .height(120.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription =
                stringResource(
                    R.string.my_list_poster_content_description,
                    title,
                ),
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun TitleSection(
    displayTitle: String,
    subtitleTitle: String?,
) {
    Text(
        text = displayTitle,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        fontWeight = FontWeight.SemiBold,
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

@Composable
private fun MetadataRow(
    mediaType: String?,
    releaseSeason: String?,
    airingStatus: AiringStatus,
) {
    val metaParts = mutableListOf<String>()
    if (!mediaType.isNullOrBlank()) metaParts.add(mediaType)
    if (!releaseSeason.isNullOrBlank()) metaParts.add(releaseSeason)
    val statusText =
        when (airingStatus) {
            AiringStatus.CURRENTLY_AIRING ->
                stringResource(R.string.my_list_airing_status_airing)
            AiringStatus.FINISHED_AIRING ->
                stringResource(R.string.my_list_airing_status_finished)
            AiringStatus.NOT_YET_AIRED ->
                stringResource(R.string.my_list_airing_status_not_yet_aired)
            AiringStatus.UNKNOWN -> null
        }
    if (statusText != null) {
        metaParts.add(statusText)
    }
    if (metaParts.isNotEmpty()) {
        Text(
            text = metaParts.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressSection(
    watchedEpisodes: Int,
    totalEpisodes: Int?,
) {
    val totalEpText =
        totalEpisodes?.takeIf { it > 0 }?.toString()
            ?: stringResource(R.string.my_list_episodes_unknown)

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
        if (totalEpisodes != null && totalEpisodes > 0) {
            val progressFraction =
                (watchedEpisodes.toFloat() / totalEpisodes.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.spacing.progressBarHeight)
                        .clip(MaterialTheme.shapes.extraSmall),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                gapSize = MaterialTheme.spacing.none,
                drawStopIndicator = {},
            )
        }
        Text(
            text =
                stringResource(
                    R.string.my_list_episodes_format,
                    watchedEpisodes,
                    totalEpText,
                ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScoreBadge(score: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(
                    horizontal = MaterialTheme.spacing.badgePaddingHorizontal,
                    vertical = MaterialTheme.spacing.badgePaddingVertical,
                ),
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(MaterialTheme.spacing.iconSmall),
        )
        Text(
            text =
                if (score > 0) {
                    score.toString()
                } else {
                    stringResource(R.string.my_list_score_unrated)
                },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}

@StandardPreviews
@Composable
private fun AnimeCardPreview(
    @PreviewParameter(UserAnimePreviewParameterProvider::class) anime: UserAnime,
) {
    MyAnimeListTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
        ) {
            AnimeCard(
                anime = anime,
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
            )
        }
    }
}
