package com.gokova.myanimelist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.feature.auth.AuthScreen
import com.gokova.myanimelist.navigation.AuthRoute
import com.gokova.myanimelist.navigation.MainRoute
import com.gokova.myanimelist.navigation.rememberOAuthRedirectHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val redirectHandler = rememberOAuthRedirectHandler()
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
                onNavigateToOAuthUrl = { url ->
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)
                },
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
