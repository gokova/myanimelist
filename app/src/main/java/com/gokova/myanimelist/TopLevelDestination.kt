package com.gokova.myanimelist

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector
import com.gokova.myanimelist.navigation.MyListRoute
import com.gokova.myanimelist.navigation.RecommendationsRoute
import com.gokova.myanimelist.navigation.TasteRoute
import kotlin.reflect.KClass

data class TopLevelDestination<T : Any>(
    val route: T,
    val routeClass: KClass<out T>,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val labelTextId: Int,
)

val topLevelDestinations: List<TopLevelDestination<out Any>> =
    listOf(
        TopLevelDestination(
            route = MyListRoute,
            routeClass = MyListRoute::class,
            selectedIcon = Icons.AutoMirrored.Filled.List,
            unselectedIcon = Icons.AutoMirrored.Filled.List,
            labelTextId = R.string.nav_my_list,
        ),
        TopLevelDestination(
            route = TasteRoute,
            routeClass = TasteRoute::class,
            selectedIcon = Icons.Filled.Favorite,
            unselectedIcon = Icons.Outlined.FavoriteBorder,
            labelTextId = R.string.nav_my_taste,
        ),
        TopLevelDestination(
            route = RecommendationsRoute,
            routeClass = RecommendationsRoute::class,
            selectedIcon = Icons.Filled.Star,
            unselectedIcon = Icons.Filled.Star,
            labelTextId = R.string.nav_recommendations,
        ),
    )
