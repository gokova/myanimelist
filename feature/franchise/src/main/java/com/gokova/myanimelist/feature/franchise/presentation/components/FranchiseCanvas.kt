package com.gokova.myanimelist.feature.franchise.presentation.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.MotionTokens
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseConnectionCurve
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraphLayout
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNode
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNodeLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.min

private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 2.5f

data class FranchiseViewportTriggers(
    val recenter: Int = 0,
    val fit: Int = 0,
    val zoomIn: Int = 0,
    val zoomOut: Int = 0,
)

private data class ViewportTransform(
    val scale: Float,
    val offset: Offset,
)

private class FranchiseViewportState(
    val scale: Float,
    val offset: Offset,
    val onGestureTransform: (pan: Offset, zoom: Float) -> Unit,
)

@Composable
fun FranchiseCanvas(
    layout: FranchiseGraphLayout,
    viewportTriggers: FranchiseViewportTriggers,
    onNodeClick: (FranchiseNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val viewportHeightPx = constraints.maxHeight.toFloat()

        val viewportState =
            rememberFranchiseViewportState(
                layout = layout,
                viewportTriggers = viewportTriggers,
                density = density,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
            )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            viewportState.onGestureTransform(pan, zoom)
                        }
                    },
        ) {
            FranchiseConnectionsCanvas(
                connections = layout.connections,
                viewportState = viewportState,
                density = density.density,
                primaryColor = primaryColor,
            )

            FranchiseNodesLayer(
                nodeLayouts = layout.nodeLayouts,
                viewportState = viewportState,
                density = density.density,
                onNodeClick = onNodeClick,
            )
        }
    }
}

@Composable
private fun FranchiseConnectionsCanvas(
    connections: List<FranchiseConnectionCurve>,
    viewportState: FranchiseViewportState,
    density: Float,
    primaryColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 2.dp.toPx() * viewportState.scale.coerceIn(0.75f, 1.5f)

        for (conn in connections) {
            val startX = (conn.startXDp * density) * viewportState.scale + viewportState.offset.x
            val startY = (conn.startYDp * density) * viewportState.scale + viewportState.offset.y
            val endX = (conn.endXDp * density) * viewportState.scale + viewportState.offset.x
            val endY = (conn.endYDp * density) * viewportState.scale + viewportState.offset.y

            val midY = (startY + endY) / 2f
            val path =
                Path().apply {
                    moveTo(startX, startY)
                    cubicTo(startX, midY, endX, midY, endX, endY)
                }

            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.8f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            val arrowSize = 8.dp.toPx() * viewportState.scale.coerceIn(0.75f, 1.5f)
            val arrowPath =
                Path().apply {
                    moveTo(endX - arrowSize, endY - arrowSize)
                    lineTo(endX, endY)
                    lineTo(endX + arrowSize, endY - arrowSize)
                }
            drawPath(
                path = arrowPath,
                color = primaryColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun FranchiseNodesLayer(
    nodeLayouts: List<FranchiseNodeLayout>,
    viewportState: FranchiseViewportState,
    density: Float,
    onNodeClick: (FranchiseNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        for (nodeLayout in nodeLayouts) {
            val nodePxX = nodeLayout.xDp * density
            val nodePxY = nodeLayout.yDp * density
            FranchiseNodeCard(
                node = nodeLayout.node,
                onClick = onNodeClick,
                modifier =
                    Modifier
                        .graphicsLayer {
                            translationX =
                                nodePxX * viewportState.scale + viewportState.offset.x
                            translationY =
                                nodePxY * viewportState.scale + viewportState.offset.y
                            scaleX = viewportState.scale
                            scaleY = viewportState.scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }.size(nodeLayout.widthDp.dp, nodeLayout.heightDp.dp),
            )
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun rememberFranchiseViewportState(
    layout: FranchiseGraphLayout,
    viewportTriggers: FranchiseViewportTriggers,
    density: Density,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
): FranchiseViewportState {
    var transform by remember { mutableStateOf(ViewportTransform(1f, Offset.Zero)) }
    val coroutineScope = rememberCoroutineScope()
    var animJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(viewportTriggers.recenter) {
        if (viewportTriggers.recenter > 0 && layout.originNodeLayout != null) {
            animJob?.cancel()
            val target =
                computeRecenterTransform(
                    origin = layout.originNodeLayout,
                    scale = transform.scale,
                    density = density.density,
                    viewportWidthPx = viewportWidthPx,
                    viewportHeightPx = viewportHeightPx,
                )
            animJob =
                coroutineScope.launch {
                    animateViewport(
                        start = transform,
                        target = target,
                        durationMillis = MotionTokens.DURATION_CHART_RECENTER,
                        easing = MotionTokens.EasingEnter,
                    ) { updated -> transform = updated }
                    animJob = null
                }
        }
    }

    LaunchedEffect(viewportTriggers.fit) {
        if (viewportTriggers.fit > 0 && layout.contentWidthDp > 0 && layout.contentHeightDp > 0) {
            animJob?.cancel()
            val target =
                computeFitTransform(
                    contentWidthDp = layout.contentWidthDp,
                    contentHeightDp = layout.contentHeightDp,
                    density = density.density,
                    viewportWidthPx = viewportWidthPx,
                    viewportHeightPx = viewportHeightPx,
                )
            animJob =
                coroutineScope.launch {
                    animateViewport(
                        start = transform,
                        target = target,
                        durationMillis = MotionTokens.DURATION_STANDARD,
                        easing = MotionTokens.EasingStandard,
                    ) { updated -> transform = updated }
                    animJob = null
                }
        }
    }

    LaunchedEffect(viewportTriggers.zoomIn) {
        if (viewportTriggers.zoomIn > 0) {
            animJob?.cancel()
            val target =
                computeZoomTransform(
                    current = transform,
                    multiplier = 1.25f,
                    viewportWidthPx = viewportWidthPx,
                    viewportHeightPx = viewportHeightPx,
                )
            animJob =
                coroutineScope.launch {
                    animateViewport(
                        start = transform,
                        target = target,
                        durationMillis = MotionTokens.DURATION_QUICK,
                        easing = MotionTokens.EasingStandard,
                    ) { updated -> transform = updated }
                    animJob = null
                }
        }
    }

    LaunchedEffect(viewportTriggers.zoomOut) {
        if (viewportTriggers.zoomOut > 0) {
            animJob?.cancel()
            val target =
                computeZoomTransform(
                    current = transform,
                    multiplier = 0.8f,
                    viewportWidthPx = viewportWidthPx,
                    viewportHeightPx = viewportHeightPx,
                )
            animJob =
                coroutineScope.launch {
                    animateViewport(
                        start = transform,
                        target = target,
                        durationMillis = MotionTokens.DURATION_QUICK,
                        easing = MotionTokens.EasingStandard,
                    ) { updated -> transform = updated }
                    animJob = null
                }
        }
    }

    return remember(transform) {
        FranchiseViewportState(
            scale = transform.scale,
            offset = transform.offset,
            onGestureTransform = { pan, zoom ->
                animJob?.cancel()
                animJob = null
                val newScale = (transform.scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                val factor = newScale / transform.scale
                val newOffset =
                    Offset(
                        x = transform.offset.x * factor + pan.x,
                        y = transform.offset.y * factor + pan.y,
                    )
                transform = ViewportTransform(newScale, newOffset)
            },
        )
    }
}

private fun computeRecenterTransform(
    origin: FranchiseNodeLayout,
    scale: Float,
    density: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
): ViewportTransform {
    val originCenterXPx = (origin.xDp + origin.widthDp / 2f) * density * scale
    val originCenterYPx = (origin.yDp + origin.heightDp / 2f) * density * scale
    return ViewportTransform(
        scale = scale,
        offset =
            Offset(
                x = viewportWidthPx / 2f - originCenterXPx,
                y = viewportHeightPx / 2f - originCenterYPx,
            ),
    )
}

private fun computeFitTransform(
    contentWidthDp: Float,
    contentHeightDp: Float,
    density: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
): ViewportTransform {
    val contentWidthPx = contentWidthDp * density
    val contentHeightPx = contentHeightDp * density
    val targetScale =
        min(
            viewportWidthPx / contentWidthPx,
            viewportHeightPx / contentHeightPx,
        ).coerceIn(MIN_SCALE, 1.2f)
    val scaledWidth = contentWidthPx * targetScale
    val scaledHeight = contentHeightPx * targetScale
    return ViewportTransform(
        scale = targetScale,
        offset =
            Offset(
                x = (viewportWidthPx - scaledWidth) / 2f,
                y = (viewportHeightPx - scaledHeight) / 2f,
            ),
    )
}

private fun computeZoomTransform(
    current: ViewportTransform,
    multiplier: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
): ViewportTransform {
    val targetScale = (current.scale * multiplier).coerceIn(MIN_SCALE, MAX_SCALE)
    val scaleFactor = targetScale / current.scale
    val cx = viewportWidthPx / 2f
    val cy = viewportHeightPx / 2f
    return ViewportTransform(
        scale = targetScale,
        offset =
            Offset(
                x = cx - (cx - current.offset.x) * scaleFactor,
                y = cy - (cy - current.offset.y) * scaleFactor,
            ),
    )
}

private suspend fun animateViewport(
    start: ViewportTransform,
    target: ViewportTransform,
    durationMillis: Int,
    easing: Easing,
    onUpdate: (ViewportTransform) -> Unit,
) {
    val spec = tween<Float>(durationMillis = durationMillis, easing = easing)
    animate(0f, 1f, animationSpec = spec) { fraction, _ ->
        val currentScale = start.scale + (target.scale - start.scale) * fraction
        val currentOffset =
            Offset(
                x = start.offset.x + (target.offset.x - start.offset.x) * fraction,
                y = start.offset.y + (target.offset.y - start.offset.y) * fraction,
            )
        onUpdate(ViewportTransform(currentScale, currentOffset))
    }
}
