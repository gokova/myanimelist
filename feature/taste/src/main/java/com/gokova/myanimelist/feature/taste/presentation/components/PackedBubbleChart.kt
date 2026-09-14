package com.gokova.myanimelist.feature.taste.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gokova.myanimelist.core.ui.theme.BubbleColorScheme
import com.gokova.myanimelist.core.ui.theme.TasteColors
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.taste.R
import com.gokova.myanimelist.feature.taste.domain.model.TasteBubble
import kotlin.math.sqrt

private const val MIN_SCALE = 0.6f
private const val MAX_SCALE = 3.5f
private const val DEFAULT_SCALE = 1f

internal data class ChartViewport(
    val center: Offset,
    val offset: Offset,
    val scale: Float,
)

private data class RenderContext(
    val viewport: ChartViewport,
    val isDark: Boolean,
    val selectedBubbleName: String?,
    val primaryColor: Color,
    val textMeasurer: TextMeasurer,
)

data class PackedBubbleChartActions(
    val onBubbleClick: (TasteBubble) -> Unit,
    val onRecenterClick: () -> Unit,
)

private class PackedBubbleChartState(
    val scale: Float,
    val offset: Offset,
    val onTransform: (pan: Offset, zoom: Float) -> Unit,
) {
    val isTransformed: Boolean get() = scale != DEFAULT_SCALE || offset != Offset.Zero
}

@Composable
private fun rememberPackedBubbleChartState(recenterTrigger: Int): PackedBubbleChartState {
    var scale by remember { mutableFloatStateOf(DEFAULT_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(recenterTrigger) {
        scale = DEFAULT_SCALE
        offset = Offset.Zero
    }

    return remember(scale, offset) {
        PackedBubbleChartState(
            scale = scale,
            offset = offset,
            onTransform = { pan, zoom ->
                scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                offset += pan
            },
        )
    }
}

@Composable
fun PackedBubbleChart(
    bubbles: List<TasteBubble>,
    actions: PackedBubbleChartActions,
    recenterTrigger: Int,
    modifier: Modifier = Modifier,
    selectedBubble: TasteBubble? = null,
) {
    val state = rememberPackedBubbleChartState(recenterTrigger)
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary

    val chartDescription =
        pluralStringResource(R.plurals.taste_chart_description, bubbles.size, bubbles.size)
    val bubbleActions = rememberBubbleActions(bubbles, actions.onBubbleClick)
    val chartSemantics =
        Modifier.semantics {
            contentDescription = chartDescription
            customActions = bubbleActions
        }

    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val center = Offset(constraints.maxWidth / 2f, constraints.maxHeight / 2f)
        val viewport = ChartViewport(center, state.offset, state.scale)

        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(chartSemantics)
                    .chartGestures(
                        bubbles = bubbles,
                        viewport = viewport,
                        density = density,
                        onBubbleClick = actions.onBubbleClick,
                        onTransform = state.onTransform,
                    ),
        ) {
            val context =
                RenderContext(
                    viewport = viewport,
                    isDark = isDark,
                    selectedBubbleName = selectedBubble?.name,
                    primaryColor = primaryColor,
                    textMeasurer = textMeasurer,
                )
            for (bubble in bubbles) renderBubble(bubble, context)
        }

        BubbleAccessibilityOverlay(
            bubbles = bubbles,
            viewport = viewport,
            density = density,
            onBubbleClick = actions.onBubbleClick,
        )

        RecenterButton(
            isVisible = state.isTransformed,
            onRecenter = actions.onRecenterClick,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun RecenterButton(
    isVisible: Boolean,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.padding(MaterialTheme.spacing.medium),
    ) {
        FloatingActionButton(
            onClick = onRecenter,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = stringResource(R.string.taste_recenter_cd),
            )
        }
    }
}

private fun Modifier.chartGestures(
    bubbles: List<TasteBubble>,
    viewport: ChartViewport,
    density: Density,
    onBubbleClick: (TasteBubble) -> Unit,
    onTransform: (pan: Offset, zoom: Float) -> Unit,
): Modifier =
    this
        .pointerInput(bubbles, viewport) {
            detectTapGestures { tapOffset ->
                val clicked = findTappedBubble(tapOffset, bubbles, viewport, density)
                if (clicked != null) onBubbleClick(clicked)
            }
        }.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                onTransform(pan, zoom)
            }
        }

private fun findTappedBubble(
    tapOffset: Offset,
    bubbles: List<TasteBubble>,
    viewport: ChartViewport,
    density: Density,
): TasteBubble? {
    var closestBubble: TasteBubble? = null
    var minDistance = Float.MAX_VALUE

    for (bubble in bubbles) {
        val bCenter =
            with(density) {
                Offset(
                    viewport.center.x + viewport.offset.x + bubble.x.dp.toPx() * viewport.scale,
                    viewport.center.y + viewport.offset.y + bubble.y.dp.toPx() * viewport.scale,
                )
            }
        val bRadius = with(density) { bubble.radius.dp.toPx() * viewport.scale }
        val dx = tapOffset.x - bCenter.x
        val dy = tapOffset.y - bCenter.y
        val dist = sqrt(dx * dx + dy * dy)

        if (dist <= bRadius && dist < minDistance) {
            minDistance = dist
            closestBubble = bubble
        }
    }
    return closestBubble
}

private fun DrawScope.renderBubble(
    bubble: TasteBubble,
    context: RenderContext,
) {
    val vp = context.viewport
    val screenCenter =
        Offset(
            vp.center.x + vp.offset.x + bubble.x.dp.toPx() * vp.scale,
            vp.center.y + vp.offset.y + bubble.y.dp.toPx() * vp.scale,
        )
    val screenRadius = bubble.radius.dp.toPx() * vp.scale

    if (isBubbleOffscreen(screenCenter, screenRadius, size)) return

    val colors = TasteColors.getColorScheme(bubble.colorIndex, context.isDark)
    drawCircle(color = colors.container, radius = screenRadius, center = screenCenter)

    if (context.selectedBubbleName == bubble.name) {
        drawCircle(
            color = context.primaryColor,
            radius = screenRadius + 3.dp.toPx(),
            center = screenCenter,
            style = Stroke(width = 3.dp.toPx()),
        )
    }

    if (screenRadius >= 20.dp.toPx()) {
        drawBubbleLabels(bubble, screenCenter, screenRadius, colors, context.textMeasurer)
    }
}

private fun isBubbleOffscreen(
    center: Offset,
    radius: Float,
    size: Size,
): Boolean {
    val isHorizontallyOff = center.x + radius < 0 || center.x - radius > size.width
    val isVerticallyOff = center.y + radius < 0 || center.y - radius > size.height
    return isHorizontallyOff || isVerticallyOff
}

private fun DrawScope.drawBubbleLabels(
    bubble: TasteBubble,
    center: Offset,
    radius: Float,
    colors: BubbleColorScheme,
    textMeasurer: TextMeasurer,
) {
    val maxTextWidth = (radius * 1.6f).toInt()
    val titleFontSize =
        when {
            radius >= 55.dp.toPx() -> 14.sp
            radius >= 35.dp.toPx() -> 12.sp
            else -> 10.sp
        }
    val countFontSize = if (radius >= 55.dp.toPx()) 11.sp else 9.sp

    val titleResult =
        textMeasurer.measure(
            text = bubble.name,
            style =
                TextStyle(
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onContainer,
                ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = maxTextWidth),
        )

    val countResult =
        textMeasurer.measure(
            text = "(${bubble.count})",
            style =
                TextStyle(
                    fontSize = countFontSize,
                    fontWeight = FontWeight.Medium,
                    color = colors.onContainer.copy(alpha = 0.85f),
                ),
            maxLines = 1,
            constraints = Constraints(maxWidth = maxTextWidth),
        )

    val totalHeight = titleResult.size.height + countResult.size.height + 2
    val startY = center.y - totalHeight / 2f

    drawText(
        textLayoutResult = titleResult,
        topLeft = Offset(center.x - titleResult.size.width / 2f, startY),
    )
    drawText(
        textLayoutResult = countResult,
        topLeft =
            Offset(
                center.x - countResult.size.width / 2f,
                startY + titleResult.size.height + 2,
            ),
    )
}
