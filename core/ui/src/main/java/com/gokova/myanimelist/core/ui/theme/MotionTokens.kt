package com.gokova.myanimelist.core.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Immutable

/**
 * Shared motion tokens across the application, defining restrained, intentional durations and easings.
 * Avoids bouncy overshoot and respects system animation scale settings.
 */
@Immutable
object MotionTokens {
    // Shared duration tokens (ms)
    const val DURATION_QUICK: Int = 150
    const val DURATION_MEDIUM: Int = 200
    const val DURATION_STANDARD: Int = 250

    // Poster preview overlay
    const val DURATION_POSTER_ENTER: Int = 200
    const val DURATION_POSTER_EXIT: Int = 140
    const val SCALE_POSTER_MIN: Float = 0.97f

    // Content transitions (Search/Details)
    const val DURATION_CONTENT_FADE: Int = 140

    // Interactive mutations & Recenter
    const val DURATION_PROGRESS_SEGMENT: Int = 180
    const val DURATION_ITEM_PLACEMENT: Int = 200
    const val DURATION_CHART_RECENTER: Int = 220

    // Shared Easings (no overshoot)
    val EasingStandard: Easing = FastOutSlowInEasing
    val EasingEnter: Easing = LinearOutSlowInEasing
    val EasingExit: Easing = FastOutLinearInEasing
}
