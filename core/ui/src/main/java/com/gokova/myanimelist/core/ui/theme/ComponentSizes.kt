package com.gokova.myanimelist.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Repeated semantic component dimensions shared by feature cards and detail surfaces. */
@Immutable
data class ComponentSizes(
    val posterSmallWidth: Dp = 80.dp,
    val posterSmallHeight: Dp = 120.dp,
    val detailPosterWidth: Dp = 110.dp,
    val detailPosterHeight: Dp = 165.dp,
    val carouselPosterWidth: Dp = 110.dp,
    val carouselPosterHeight: Dp = 150.dp,
    val bottomSheetPosterWidth: Dp = 56.dp,
    val bottomSheetPosterHeight: Dp = 80.dp,
    val emptyStateIconContainer: Dp = 80.dp,
    val emptyStateIcon: Dp = 40.dp,
    val segmentedSelectorHeight: Dp = 64.dp,
)

val LocalComponentSizes = staticCompositionLocalOf { ComponentSizes() }

val MaterialTheme.componentSizes: ComponentSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalComponentSizes.current
