package com.gokova.myanimelist.feature.taste.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gokova.myanimelist.core.ui.component.AnimePosterPreviewDialog
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.TasteColors
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.taste.R
import com.gokova.myanimelist.feature.taste.domain.model.TasteAnimeItem
import com.gokova.myanimelist.feature.taste.domain.model.TasteBubble

private const val BOTTOM_SHEET_MAX_HEIGHT_RATIO = 0.75f
private const val BOTTOM_SHEET_SCRIM_ALPHA = 0.32f
private val DEFAULT_MAX_HEIGHT = 600.dp
private val SHEET_CORNER_RADIUS = 32.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasteBottomSheet(
    bubble: TasteBubble,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    if (LocalInspectionMode.current) {
        TasteBottomSheetPreviewOverlay(
            bubble = bubble,
            modifier = modifier,
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            shape =
                RoundedCornerShape(
                    topStart = SHEET_CORNER_RADIUS,
                    topEnd = SHEET_CORNER_RADIUS,
                ),
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = modifier,
        ) {
            TasteBottomSheetContent(
                bubble = bubble,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasteBottomSheetPreviewOverlay(
    bubble: TasteBubble,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = BOTTOM_SHEET_SCRIM_ALPHA)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape =
                RoundedCornerShape(
                    topStart = SHEET_CORNER_RADIUS,
                    topEnd = SHEET_CORNER_RADIUS,
                ),
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BottomSheetDefaults.DragHandle()
                TasteBottomSheetContent(
                    bubble = bubble,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun rememberSheetNestedScrollConnection(): NestedScrollConnection =
    remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = if (available.y < 0f) available else Offset.Zero

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = available
        }
    }

@Composable
fun TasteBottomSheetContent(
    bubble: TasteBubble,
    modifier: Modifier = Modifier,
) {
    var previewAnime by remember { mutableStateOf<TasteAnimeItem?>(null) }
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
    val maxHeight =
        if (screenHeightDp > 0.dp) {
            screenHeightDp * BOTTOM_SHEET_MAX_HEIGHT_RATIO
        } else {
            DEFAULT_MAX_HEIGHT
        }

    val nestedScrollConnection = rememberSheetNestedScrollConnection()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(horizontal = MaterialTheme.spacing.medium),
    ) {
        TasteBottomSheetHeader(bubble = bubble)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .nestedScroll(nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            items(bubble.matchingAnime, key = { it.id }) { anime ->
                TasteAnimeCard(
                    anime = anime,
                    onPosterClick = { previewAnime = anime },
                )
            }

            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            }
        }
    }

    previewAnime?.let { anime ->
        AnimePosterPreviewDialog(
            thumbnailUrl = anime.thumbnailUrl,
            largeImageUrl = anime.largeImageUrl,
            title = anime.title,
            onDismiss = { previewAnime = null },
        )
    }
}

@Composable
private fun TasteBottomSheetHeader(
    bubble: TasteBubble,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val bubbleColors = TasteColors.getColorScheme(bubble.colorIndex, isDark)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = bubble.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(bubbleColors.container)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.taste_bottom_sheet_entries,
                            bubble.count,
                            bubble.count,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = bubbleColors.onContainer,
                )
            }

            TasteScoreBadge(avgScore = bubble.averageScore)
        }
    }
}

@Composable
private fun TasteScoreBadge(
    avgScore: Double?,
    modifier: Modifier = Modifier,
) {
    val scoreText =
        if (avgScore != null) {
            stringResource(R.string.taste_bottom_sheet_avg_score, avgScore)
        } else {
            stringResource(R.string.taste_bottom_sheet_unrated)
        }

    Box(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (avgScore != null) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = scoreText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun TasteAnimeCard(
    anime: TasteAnimeItem,
    modifier: Modifier = Modifier,
    onPosterClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.Top,
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
                    stringResource(R.string.taste_zoom_poster_description, anime.title)
                } else {
                    anime.title
                }

            AsyncImage(
                model = anime.thumbnailUrl,
                contentDescription = contentDesc,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(width = 56.dp, height = 80.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(clickableModifier),
            )

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

            AnimeCardContent(anime = anime, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AnimeCardContent(
    anime: TasteAnimeItem,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = anime.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        val totalEpStr = anime.totalEpisodes?.toString() ?: "??"
        Text(
            text =
                stringResource(
                    R.string.taste_episodes_format,
                    anime.watchedEpisodes,
                    totalEpStr,
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val scoreText =
                if (anime.userScore > 0) {
                    stringResource(R.string.taste_score_format, anime.userScore)
                } else {
                    stringResource(R.string.taste_score_unrated)
                }
            Text(
                text = scoreText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            TasteStatusChip(status = anime.userStatus)
        }
    }
}

@Suppress("MagicNumber")
@StandardPreviews
@Composable
private fun TasteBottomSheetPreview() {
    MyAnimeListTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TasteBottomSheetContent(
                bubble =
                    TasteBubble(
                        name = "Adventure",
                        count = 39,
                        radius = 80f,
                        x = 0f,
                        y = 0f,
                        colorIndex = 1,
                        averageScore = 8.4,
                        matchingAnime =
                            listOf(
                                TasteAnimeItem(1, "Naruto", null, null, 8, "completed", 220, 220),
                                TasteAnimeItem(2, "One Piece", null, null, 9, "watching", 0, 196),
                                TasteAnimeItem(3, "Berserk", null, null, 0, "plan_to_watch", 25, 0),
                                TasteAnimeItem(4, ".hack//Sign", null, null, 6, "completed", 26, 26),
                                TasteAnimeItem(5, "Last Exile", null, null, 7, "completed", 26, 26),
                            ),
                    ),
            )
        }
    }
}
