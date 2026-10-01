package com.gokova.myanimelist

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.feature.auth.AuthScreen
import com.gokova.myanimelist.feature.details.navigation.AnimeDetailsRoute
import com.gokova.myanimelist.feature.details.presentation.AnimeDetailsScreen
import com.gokova.myanimelist.feature.profile.navigation.ProfileRoute
import com.gokova.myanimelist.feature.profile.presentation.ProfileScreen
import com.gokova.myanimelist.navigation.AuthRoute
import com.gokova.myanimelist.navigation.MainRoute
import com.gokova.myanimelist.navigation.OAuthRedirectHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val redirectHandler = OAuthRedirectHandler()
    private var fallbackCustomTabInProgress = false

    private val authTabLauncher =
        AuthTabIntent.registerActivityResultLauncher(this) { result ->
            AppLog.ui.i { "OAuth custom tab returned result code: ${result.resultCode}" }
            when (result.resultCode) {
                AuthTabIntent.RESULT_OK -> {
                    result.resultUri?.let { uri ->
                        AppLog.ui.d {
                            "OAuth redirect received scheme=${uri.scheme} host=${uri.host}"
                        }
                        redirectHandler.handleUri(
                            scheme = uri.scheme,
                            host = uri.host,
                            getQueryParameter = uri::getQueryParameter,
                        )
                    }
                }
                AuthTabIntent.RESULT_CANCELED -> {
                    AppLog.ui.i { "OAuth authentication canceled by user" }
                    redirectHandler.handleCancellation()
                }
                else -> {
                    AppLog.ui.w { "OAuth unexpected result code: ${result.resultCode}" }
                    redirectHandler.handleCancellation()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        redirectHandler.handleIntent(intent)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MyAnimeListTheme {
                when (val state = uiState) {
                    is MainUiState.Loading -> {
                        LoadingScreen()
                    }
                    is MainUiState.Authenticated -> {
                        AppNavigation(
                            isLoggedIn = state.isLoggedIn,
                            avatarUrl = state.avatarUrl,
                            onLogout = viewModel::logout,
                            redirectHandler = redirectHandler,
                            onLaunchOAuth = ::launchOAuth,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // A fallback Custom Tab has no ActivityResult callback. A redirect intent proves that
        // the browser returned with a result, so do not treat this resume as cancellation.
        fallbackCustomTabInProgress = false
        redirectHandler.handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (fallbackCustomTabInProgress) {
            fallbackCustomTabInProgress = false
            if (redirectHandler.redirectResult.value == null) {
                AppLog.ui.i { "OAuth Custom Tab closed without a redirect" }
                redirectHandler.handleCancellation()
            }
        }
    }

    private fun launchOAuth(url: String) {
        AppLog.ui.i { "Initiating OAuth custom tab launch" }
        val uri = url.toUri()
        val packageName = CustomTabsClient.getPackageName(this, null)
        val isAuthTabSupported =
            packageName != null && CustomTabsClient.isAuthTabSupported(this, packageName)
        AppLog.ui.d { "OAuth tab supported: $isAuthTabSupported" }

        if (isAuthTabSupported) {
            val builder = AuthTabIntent.Builder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setEphemeralBrowsingEnabled(true)
            }
            val authTabIntent = builder.build()
            authTabIntent.launch(authTabLauncher, uri, OAuthRedirectHandler.EXPECTED_SCHEME)
        } else {
            fallbackCustomTabInProgress = true
            val builder = CustomTabsIntent.Builder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setEphemeralBrowsingEnabled(true)
            }
            val customTabsIntent = builder.build()
            packageName?.let { customTabsIntent.intent.setPackage(it) }
            customTabsIntent.launchUrl(this, uri)
        }
    }
}

@Composable
fun AppNavigation(
    isLoggedIn: Boolean,
    avatarUrl: String?,
    onLogout: () -> Unit,
    redirectHandler: OAuthRedirectHandler,
    onLaunchOAuth: (String) -> Unit,
) {
    val navController = rememberNavController()

    TrackNavigationChanges(navController)
    ObserveAuthStateNavigation(isLoggedIn = isLoggedIn, navController = navController)

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) MainRoute else AuthRoute,
    ) {
        authDestination(
            redirectHandler = redirectHandler,
            onLaunchOAuth = onLaunchOAuth,
            onAuthSuccess = {
                if (navController.currentDestination?.hasRoute<AuthRoute>() == true) {
                    AppLog.ui.i { "Authentication succeeded, navigating to MainRoute" }
                    navController.navigate(MainRoute) {
                        popUpTo(AuthRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
        )
        composable<MainRoute> {
            MainScreen(
                avatarUrl = avatarUrl,
                onProfileClick = { navController.navigate(ProfileRoute) },
                onAnimeClick = { animeId ->
                    navController.navigate(AnimeDetailsRoute(animeId))
                },
            )
        }
        composable<ProfileRoute> {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutConfirm = onLogout,
            )
        }
        composable<AnimeDetailsRoute> {
            AnimeDetailsScreen(
                onBackClick = { navController.popBackStack() },
                onAnimeClick = { animeId ->
                    navController.navigate(AnimeDetailsRoute(animeId))
                },
            )
        }
    }
}

private fun NavGraphBuilder.authDestination(
    redirectHandler: OAuthRedirectHandler,
    onLaunchOAuth: (String) -> Unit,
    onAuthSuccess: () -> Unit,
) {
    composable<AuthRoute> {
        val redirectResult by redirectHandler.redirectResult
        AuthScreen(
            redirectResult = redirectResult,
            onRedirectResultConsumed = redirectHandler::consumeResult,
            onNavigateToOAuthUrl = onLaunchOAuth,
            onAuthSuccess = onAuthSuccess,
        )
    }
}

@Composable
private fun ObserveAuthStateNavigation(
    isLoggedIn: Boolean,
    navController: NavController,
) {
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            AppLog.ui.i { "User not logged in, redirecting to AuthRoute" }
            navController.navigate(AuthRoute) {
                popUpTo(MainRoute) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            val isAtAuth = navController.currentDestination?.hasRoute<AuthRoute>() == true
            if (isAtAuth) {
                AppLog.ui.i { "User logged in, redirecting to MainRoute" }
                navController.navigate(MainRoute) {
                    popUpTo(AuthRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
}

@Composable
private fun TrackNavigationChanges(navController: NavController) {
    DisposableEffect(navController) {
        val listener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                AppLog.ui.i { "Navigated to destination: ${destination.route}" }
            }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
