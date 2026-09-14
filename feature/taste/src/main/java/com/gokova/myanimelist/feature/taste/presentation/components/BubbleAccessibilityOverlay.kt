package com.gokova.myanimelist.feature.taste.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.feature.taste.R
import com.gokova.myanimelist.feature.taste.domain.model.TasteBubble

@Composable
internal fun BubbleAccessibilityOverlay(
    bubbles: List<TasteBubble>,
    viewport: ChartViewport,
    density: Density,
    onBubbleClick: (TasteBubble) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { isTraversalGroup = true },
    ) {
        for (bubble in bubbles) {
            BubbleAccessibilityNode(
                bubble = bubble,
                viewport = viewport,
                density = density,
                onBubbleClick = onBubbleClick,
            )
        }
    }
}

@Composable
private fun BubbleAccessibilityNode(
    bubble: TasteBubble,
    viewport: ChartViewport,
    density: Density,
    onBubbleClick: (TasteBubble) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bubbleCenter =
        with(density) {
            Offset(
                viewport.center.x + viewport.offset.x + bubble.x.dp.toPx() * viewport.scale,
                viewport.center.y + viewport.offset.y + bubble.y.dp.toPx() * viewport.scale,
            )
        }
    val bubbleRadiusPx =
        with(density) { (bubble.radius.dp.toPx() * viewport.scale).coerceAtLeast(1f) }
    val sizeDp = with(density) { (bubbleRadiusPx * 2f).toDp() }
    val leftDp = with(density) { (bubbleCenter.x - bubbleRadiusPx).toDp() }
    val topDp = with(density) { (bubbleCenter.y - bubbleRadiusPx).toDp() }

    val description =
        if (bubble.averageScore != null) {
            pluralStringResource(
                R.plurals.taste_bubble_description_with_score,
                bubble.count,
                bubble.name,
                bubble.count,
                bubble.averageScore,
            )
        } else {
            pluralStringResource(
                R.plurals.taste_bubble_description,
                bubble.count,
                bubble.name,
                bubble.count,
            )
        }
    val actionLabel = stringResource(R.string.taste_bubble_action, bubble.name)

    Box(
        modifier =
            modifier
                .offset(x = leftDp, y = topDp)
                .size(sizeDp)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                    onClick(label = actionLabel) {
                        onBubbleClick(bubble)
                        true
                    }
                    customActions =
                        listOf(
                            CustomAccessibilityAction(label = actionLabel) {
                                onBubbleClick(bubble)
                                true
                            },
                        )
                },
    )
}

@Composable
internal fun rememberBubbleActions(
    bubbles: List<TasteBubble>,
    onBubbleClick: (TasteBubble) -> Unit,
): List<CustomAccessibilityAction> =
    remember(bubbles, onBubbleClick) {
        bubbles.map { bubble ->
            CustomAccessibilityAction(label = bubble.name) {
                onBubbleClick(bubble)
                true
            }
        }
    }
