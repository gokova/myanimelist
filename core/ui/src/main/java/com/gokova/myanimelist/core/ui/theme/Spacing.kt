package com.gokova.myanimelist.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Consistent spacing tokens across the application.
 * All paddings, margins, gutters, and touch targets should reference these tokens.
 */
@Immutable
data class Spacing(
    val none: Dp = 0.dp,
    val micro: Dp = 2.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val iconSmall: Dp = 12.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val extraExtraLarge: Dp = 48.dp,
    val huge: Dp = 64.dp,
    // Semantic tokens
    val screenHorizontal: Dp = 24.dp,
    val minTouchTarget: Dp = 48.dp,
    val cardPadding: Dp = 16.dp,
    val badgePaddingHorizontal: Dp = 6.dp,
    val badgePaddingVertical: Dp = 2.dp,
    val progressBarHeight: Dp = 6.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
