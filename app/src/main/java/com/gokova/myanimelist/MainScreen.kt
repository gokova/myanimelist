package com.gokova.myanimelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.gokova.myanimelist.navigation.MyListRoute
import com.gokova.myanimelist.navigation.RecommendationsRoute
import com.gokova.myanimelist.navigation.TasteRoute

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

    Scaffold(
        topBar = { MainTopAppBar(onAvatarClick = { showLogoutDialog = true }) },
        bottomBar = { AppBottomBar(navController) },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = MyListRoute,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable<MyListRoute> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.screen_my_list_placeholder))
                }
            }
            composable<TasteRoute> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.screen_taste_placeholder))
                }
            }
            composable<RecommendationsRoute> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.screen_recommendations_placeholder))
                }
            }
        }
    }
}

@Composable
fun AppBottomBar(navController: NavHostController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        topLevelDestinations.forEach { destination ->
            val selected =
                currentDestination?.hierarchy?.any {
                    it.hasRoute(destination.routeClass)
                } == true

            val label = stringResource(destination.labelTextId)

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
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

@StandardPreviews
@Composable
fun MainScreenPreview() {
    MyAnimeListTheme {
        MainScreen()
    }
}
