package com.gokova.myanimelist.feature.mylist.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gokova.myanimelist.core.ui.component.badge.PersonalScoreBadge
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.componentSizes
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.mylist.R
import com.gokova.myanimelist.feature.mylist.domain.model.AiringStatus
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnimeStatus

@Composable
fun AnimeCard(
    anime: UserAnime,
    modifier: Modifier = Modifier,
    actions: AnimeCardActions = AnimeCardActions(),
    isSavingProgress: Boolean = false,
) {
    val cardClickableModifier =
        if (actions.onCardClick != null) {
            Modifier.clickable(role = Role.Button, onClick = actions.onCardClick)
        } else {
            Modifier
        }

    Card(
        modifier = modifier.fillMaxWidth().then(cardClickableModifier),
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
                imageUrl = anime.thumbnailUrl,
                title = anime.displayTitle,
                onPosterClick = actions.onPosterClick,
            )

            AnimeCardDetails(
                anime = anime,
                actions = actions,
                isSavingProgress = isSavingProgress,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AnimeCardDetails(
    anime: UserAnime,
    actions: AnimeCardActions,
    isSavingProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val useCompactStatusChip = maxWidth < 160.dp
        Column(
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
                PersonalScoreBadge(score = anime.userScore)
                val isKnownTotal = anime.totalEpisodes != null && anime.totalEpisodes > 0
                val canQuickIncrement = !isKnownTotal || anime.watchedEpisodes < anime.totalEpisodes
                AnimeCardActionsRow(
                    userStatus = anime.userStatus,
                    actions = actions,
                    isSavingProgress = isSavingProgress,
                    useCompactStatusChip = useCompactStatusChip,
                    canQuickIncrement = canQuickIncrement,
                )
            }
        }
    }
}

@Composable
private fun AnimeCardActionsRow(
    userStatus: UserAnimeStatus,
    actions: AnimeCardActions,
    isSavingProgress: Boolean,
    useCompactStatusChip: Boolean,
    canQuickIncrement: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.none),
    ) {
        if (userStatus == UserAnimeStatus.WATCHING &&
            actions.onQuickIncrement != null
        ) {
            QuickIncrementButton(
                isSaving = isSavingProgress,
                enabled = canQuickIncrement,
                onClick = actions.onQuickIncrement,
            )
        }

        if (actions.onEditClick != null) {
            IconButton(
                onClick = actions.onEditClick,
                enabled = !isSavingProgress,
                modifier = Modifier.size(MaterialTheme.spacing.minTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription =
                        stringResource(
                            com.gokova.myanimelist.core.ui.R.string.edit_list_status_edit_button,
                        ),
                    tint =
                        if (isSavingProgress) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        AnimeStatusChip(
            status = userStatus,
            compact = useCompactStatusChip,
        )
    }
}

@Composable
private fun PosterThumbnail(
    imageUrl: String?,
    title: String,
    onPosterClick: (() -> Unit)?,
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
    val contentDescRes =
        if (onPosterClick != null) {
            R.string.my_list_zoom_poster_description
        } else {
            R.string.my_list_poster_content_description
        }
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(surfaceVariant) { ColorPainter(surfaceVariant) }

    AsyncImage(
        model = imageUrl,
        contentDescription = stringResource(contentDescRes, title),
        placeholder = placeholderPainter,
        error = placeholderPainter,
        fallback = placeholderPainter,
        modifier =
            Modifier
                .width(MaterialTheme.componentSizes.posterSmallWidth)
                .height(MaterialTheme.componentSizes.posterSmallHeight)
                .clip(MaterialTheme.shapes.small)
                .then(clickableModifier),
        contentScale = ContentScale.Crop,
    )
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
            text = metaParts.joinToString(" \u00B7 "),
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
