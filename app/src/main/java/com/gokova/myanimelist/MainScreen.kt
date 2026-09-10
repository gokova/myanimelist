package com.gokova.myanimelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gokova.myanimelist.components.LogoutConfirmationDialog
import com.gokova.myanimelist.components.MainTopAppBar
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.mylist.presentation.MyListScreen
import com.gokova.myanimelist.navigation.MyListRoute
import com.gokova.myanimelist.navigation.RecommendationsRoute
import com.gokova.myanimelist.navigation.TasteRoute
import kotlin.reflect.KClass

@Composable
fun MainScreen(onLogoutConfirm: () -> Unit = {}) {
    val navController = rememberNavController()
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogoutConfirm()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val matchedDestination =
        topLevelDestinations.firstOrNull { destination ->
            currentDestination?.hierarchy?.any {
                it.hasRoute(destination.routeClass)
            } == true
        }
    val selectedRouteClass = matchedDestination?.routeClass

    MainContent(
        selectedRouteClass = selectedRouteClass,
        onNavigateToDestination = { destination ->
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        onAvatarClick = { showLogoutDialog = true },
    ) {
        MainNavHost(navController = navController)
    }
}

@Composable
private fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = MyListRoute,
        modifier = modifier,
    ) {
        composable<MyListRoute> {
            MyListScreen()
        }
        composable<TasteRoute> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.screen_taste_placeholder))
            }
        }
        composable<RecommendationsRoute> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.screen_recommendations_placeholder))
            }
        }
    }
}

private val EXPANDED_WIDTH_BREAKPOINT = 600.dp

@Composable
fun MainContent(
    selectedRouteClass: KClass<out Any>?,
    onNavigateToDestination: (TopLevelDestination<out Any>) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val isExpanded = screenWidthDp >= EXPANDED_WIDTH_BREAKPOINT

    if (isExpanded) {
        Row(modifier = modifier.fillMaxSize()) {
            AppNavigationRail(
                selectedRouteClass = selectedRouteClass,
                onNavigateToDestination = onNavigateToDestination,
            )
            Scaffold(
                topBar = { MainTopAppBar(onAvatarClick = onAvatarClick) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    content()
                }
            }
        }
    } else {
        Scaffold(
            topBar = { MainTopAppBar(onAvatarClick = onAvatarClick) },
            bottomBar = {
                AppBottomBar(
                    selectedRouteClass = selectedRouteClass,
                    onNavigateToDestination = onNavigateToDestination,
                )
            },
            modifier = modifier,
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                content()
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    selectedRouteClass: KClass<out Any>?,
    onNavigateToDestination: (TopLevelDestination<out Any>) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        topLevelDestinations.forEach { destination ->
            val selected = selectedRouteClass == destination.routeClass
            val label = stringResource(destination.labelTextId)

            NavigationRailItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector =
                            if (selected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            },
                        contentDescription = label,
                    )
                },
                colors =
                    NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
    }
}

@Composable
fun AppBottomBar(
    selectedRouteClass: KClass<out Any>?,
    onNavigateToDestination: (TopLevelDestination<out Any>) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        topLevelDestinations.forEach { destination ->
            val selected = selectedRouteClass == destination.routeClass
            val label = stringResource(destination.labelTextId)

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector =
                            if (selected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            },
                        contentDescription = label,
                    )
                },
                label = { Text(label) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
    }
}

class MainDestinationPreviewProvider : PreviewParameterProvider<TopLevelDestination<out Any>> {
    override val values: Sequence<TopLevelDestination<out Any>> =
        topLevelDestinations.asSequence()

    override fun getDisplayName(index: Int): String? =
        when (topLevelDestinations.getOrNull(index)?.routeClass) {
            MyListRoute::class -> "My List"
            TasteRoute::class -> "My Taste"
            RecommendationsRoute::class -> "Recommendations"
            else -> null
        }
}

@StandardPreviews
@Composable
fun MainScreenPreview(
    @PreviewParameter(MainDestinationPreviewProvider::class)
    destination: TopLevelDestination<out Any>,
) {
    MyAnimeListTheme {
        MainContent(
            selectedRouteClass = destination.routeClass,
            onNavigateToDestination = {},
            onAvatarClick = {},
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(destination.labelTextId),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
