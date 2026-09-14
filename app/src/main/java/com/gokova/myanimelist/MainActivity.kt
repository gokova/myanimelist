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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.feature.auth.AuthScreen
import com.gokova.myanimelist.navigation.AuthRoute
import com.gokova.myanimelist.navigation.MainRoute
import com.gokova.myanimelist.navigation.OAuthRedirectHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val redirectHandler = OAuthRedirectHandler()

    private val authTabLauncher =
        AuthTabIntent.registerActivityResultLauncher(this) { result ->
            when (result.resultCode) {
                AuthTabIntent.RESULT_OK -> {
                    result.resultUri?.let { uri ->
                        redirectHandler.handleUri(
                            scheme = uri.scheme,
                            host = uri.host,
                            getQueryParameter = uri::getQueryParameter,
                        )
                    }
                }
                AuthTabIntent.RESULT_CANCELED -> {
                    redirectHandler.handleCancellation()
                }
                else -> {
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
        redirectHandler.handleIntent(intent)
    }

    private fun launchOAuth(url: String) {
        val uri = url.toUri()
        val packageName = CustomTabsClient.getPackageName(this, null)
        val isAuthTabSupported =
            packageName != null && CustomTabsClient.isAuthTabSupported(this, packageName)

        if (isAuthTabSupported) {
            val builder = AuthTabIntent.Builder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setEphemeralBrowsingEnabled(true)
            }
            val authTabIntent = builder.build()
            authTabIntent.launch(authTabLauncher, uri, OAuthRedirectHandler.EXPECTED_SCHEME)
        } else {
            val builder = CustomTabsIntent.Builder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setEphemeralBrowsingEnabled(true)
            }
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(this, uri)
        }
    }
}

@Composable
fun AppNavigation(
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
    redirectHandler: OAuthRedirectHandler,
    onLaunchOAuth: (String) -> Unit,
) {
    val navController = rememberNavController()
    val redirectResult by redirectHandler.redirectResult

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate(AuthRoute) {
                popUpTo(MainRoute) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) MainRoute else AuthRoute,
    ) {
        composable<AuthRoute> {
            AuthScreen(
                redirectResult = redirectResult,
                onRedirectResultConsumed = redirectHandler::consumeResult,
                onNavigateToOAuthUrl = onLaunchOAuth,
                onAuthSuccess = {
                    navController.navigate(MainRoute) {
                        popUpTo(AuthRoute) { inclusive = true }
                    }
                },
            )
        }
        composable<MainRoute> {
            MainScreen(
                onLogoutConfirm = onLogout,
            )
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
