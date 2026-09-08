package com.gokova.myanimelist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector
import com.gokova.myanimelist.navigation.MyListRoute
import com.gokova.myanimelist.navigation.RecommendationsRoute
import com.gokova.myanimelist.navigation.TasteRoute

data class TopLevelDestination(
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
)

val topLevelDestinations =
    listOf(
        TopLevelDestination(
            route = MyListRoute,
            selectedIcon = Icons.AutoMirrored.Filled.List,
            unselectedIcon = Icons.AutoMirrored.Filled.List,
            label = "My List",
        ),
        TopLevelDestination(
            route = TasteRoute,
            selectedIcon = Icons.Filled.Favorite,
            unselectedIcon = Icons.Outlined.FavoriteBorder,
            label = "My Taste",
        ),
        TopLevelDestination(
            route = RecommendationsRoute,
            selectedIcon = Icons.Filled.Star,
            unselectedIcon = Icons.Filled.Star,
            label = "Recommendations",
        ),
    )
