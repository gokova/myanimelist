package com.gokova.myanimelist.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gokova.myanimelist.core.ui.R
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MotionTokens
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import kotlinx.coroutines.launch

private const val SCRIM_ALPHA = 0.65f
private const val POSTER_SCREEN_RATIO = 0.82f
private const val POSTER_ASPECT_RATIO = 2f / 3f
private val FALLBACK_POSTER_WIDTH = 260.dp

@Immutable
private data class PosterPreviewData(
    val thumbnailUrl: String?,
    val largeImageUrl: String?,
    val title: String,
)

@Composable
fun AnimePosterPreviewDialog(
    thumbnailUrl: String?,
    largeImageUrl: String?,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val poster =
        remember(thumbnailUrl, largeImageUrl, title) {
            PosterPreviewData(thumbnailUrl, largeImageUrl, title)
        }

    if (LocalInspectionMode.current) {
        AnimePosterPreviewSurface(
            poster = poster,
            animatedAlpha = 1f,
            animatedScale = 1f,
            onClose = onDismiss,
            modifier = modifier,
        )
    } else {
        AnimePosterPreviewDialogWindow(
            poster = poster,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

@Composable
private fun AnimePosterPreviewDialogWindow(
    poster: PosterPreviewData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatable = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }

    val dismissWithAnimation: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            scope.launch {
                try {
                    animatable.animateTo(
                        targetValue = 0f,
                        animationSpec =
                            tween(
                                durationMillis = MotionTokens.DURATION_POSTER_EXIT,
                                easing = MotionTokens.EasingExit,
                            ),
                    )
                } finally {
                    onDismiss()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        animatable.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = MotionTokens.DURATION_POSTER_ENTER,
                    easing = MotionTokens.EasingEnter,
                ),
        )
    }

    Dialog(
        onDismissRequest = dismissWithAnimation,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        val scale =
            MotionTokens.SCALE_POSTER_MIN +
                (1f - MotionTokens.SCALE_POSTER_MIN) * animatable.value
        AnimePosterPreviewSurface(
            poster = poster,
            animatedAlpha = animatable.value,
            animatedScale = scale,
            onClose = dismissWithAnimation,
            modifier = modifier,
        )
    }
}

@Composable
fun AnimePosterPreviewOverlay(
    thumbnailUrl: String?,
    largeImageUrl: String?,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val poster =
        remember(thumbnailUrl, largeImageUrl, title) {
            PosterPreviewData(thumbnailUrl, largeImageUrl, title)
        }
    AnimePosterPreviewSurface(
        poster = poster,
        animatedAlpha = 1f,
        animatedScale = 1f,
        onClose = onDismiss,
        modifier = modifier,
    )
}

@Composable
private fun AnimePosterPreviewSurface(
    poster: PosterPreviewData,
    animatedAlpha: Float,
    animatedScale: Float,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA * animatedAlpha),
                ).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose,
                ),
        contentAlignment = Alignment.Center,
    ) {
        PosterContentCard(
            poster = poster,
            onClose = onClose,
            modifier =
                Modifier.graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    alpha = animatedAlpha
                },
        )
    }
}

@Composable
private fun PosterContentCard(
    poster: PosterPreviewData,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .posterSizing()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            PosterImages(poster = poster)
        }

        PosterCloseButton(
            onClose = onClose,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun Modifier.posterSizing(): Modifier {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val screenWidthDp = with(density) { containerSize.width.toDp() }
    val screenHeightDp = with(density) { containerSize.height.toDp() }

    return if (screenWidthDp > 0.dp && screenHeightDp > 0.dp) {
        if (screenWidthDp > screenHeightDp) {
            this
                .height(screenHeightDp * POSTER_SCREEN_RATIO)
                .aspectRatio(POSTER_ASPECT_RATIO)
        } else {
            this
                .width(screenWidthDp * POSTER_SCREEN_RATIO)
                .aspectRatio(POSTER_ASPECT_RATIO)
        }
    } else {
        this
            .width(FALLBACK_POSTER_WIDTH)
            .aspectRatio(POSTER_ASPECT_RATIO)
    }
}

@Composable
private fun PosterImages(poster: PosterPreviewData) {
    val context = LocalContext.current
    val hasDistinctLargeImage =
        !poster.largeImageUrl.isNullOrBlank() &&
            poster.largeImageUrl != poster.thumbnailUrl &&
            !poster.thumbnailUrl.isNullOrBlank()

    if (hasDistinctLargeImage) {
        AsyncImage(
            model = poster.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        AsyncImage(
            model =
                ImageRequest
                    .Builder(context)
                    .data(poster.largeImageUrl)
                    .crossfade(true)
                    .build(),
            contentDescription = poster.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(context)
                    .data(poster.largeImageUrl ?: poster.thumbnailUrl)
                    .crossfade(true)
                    .build(),
            contentDescription = poster.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PosterCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClose,
        modifier =
            modifier
                .padding(MaterialTheme.spacing.small)
                .size(MaterialTheme.spacing.extraExtraLarge)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = CircleShape,
                ),
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.poster_preview_close),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

@StandardPreviews
@Composable
private fun AnimePosterPreviewOverlayPreview() {
    MyAnimeListTheme {
        AnimePosterPreviewOverlay(
            thumbnailUrl = null,
            largeImageUrl = null,
            title = "Preview Anime Title",
            onDismiss = {},
        )
    }
}
