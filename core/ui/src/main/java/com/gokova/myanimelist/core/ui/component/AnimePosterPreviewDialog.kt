package com.gokova.myanimelist.core.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val SCRIM_ALPHA = 0.65f
private const val POSTER_SCREEN_RATIO = 0.82f
private const val POSTER_ASPECT_RATIO = 2f / 3f
private const val DISMISS_DELAY_MS = 150L
private val FALLBACK_POSTER_WIDTH = 260.dp

@Composable
fun AnimePosterPreviewDialog(
    thumbnailUrl: String?,
    largeImageUrl: String?,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        AnimePosterPreviewOverlay(
            thumbnailUrl = thumbnailUrl,
            largeImageUrl = largeImageUrl,
            title = title,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                ),
        ) {
            AnimePosterPreviewOverlay(
                thumbnailUrl = thumbnailUrl,
                largeImageUrl = largeImageUrl,
                title = title,
                onDismiss = onDismiss,
                modifier = modifier,
            )
        }
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
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.7f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        label = "poster_spring_scale",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "poster_alpha",
    )

    fun dismissWithAnimation() {
        scope.launch {
            isVisible = false
            delay(DISMISS_DELAY_MS.milliseconds)
            onDismiss()
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA * animatedAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissWithAnimation() },
                ),
        contentAlignment = Alignment.Center,
    ) {
        PosterContentCard(
            thumbnailUrl = thumbnailUrl,
            largeImageUrl = largeImageUrl,
            title = title,
            onClose = { dismissWithAnimation() },
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
    thumbnailUrl: String?,
    largeImageUrl: String?,
    title: String,
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
            PosterImages(
                thumbnailUrl = thumbnailUrl,
                largeImageUrl = largeImageUrl,
                title = title,
            )
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
private fun PosterImages(
    thumbnailUrl: String?,
    largeImageUrl: String?,
    title: String,
) {
    val context = LocalContext.current
    val hasDistinctLargeImage =
        !largeImageUrl.isNullOrBlank() &&
            largeImageUrl != thumbnailUrl &&
            !thumbnailUrl.isNullOrBlank()

    if (hasDistinctLargeImage) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        AsyncImage(
            model =
                ImageRequest
                    .Builder(context)
                    .data(largeImageUrl)
                    .crossfade(true)
                    .build(),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(context)
                    .data(largeImageUrl ?: thumbnailUrl)
                    .crossfade(true)
                    .build(),
            contentDescription = title,
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
