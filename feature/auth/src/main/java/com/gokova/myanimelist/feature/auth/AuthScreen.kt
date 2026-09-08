package com.gokova.myanimelist.feature.auth

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onAuthSuccess()
        }
    }

    LaunchedEffect(activity?.intent) {
        val uri = activity?.intent?.data
        android.util.Log.d("AuthScreen", "LaunchedEffect intent triggered with uri: $uri")
        if (uri != null && uri.scheme == "com.gokova.myanimelist") {
            val code = uri.getQueryParameter("code")
            android.util.Log.d("AuthScreen", "Intercepted code in LaunchedEffect: $code")
            if (code != null) {
                viewModel.handleAuthorizationCode(code)
                activity.intent.data = null
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(activity) {
        val listener =
            androidx.core.util.Consumer<Intent> { intent ->
                val uri = intent.data
                android.util.Log.d("AuthScreen", "DisposableEffect listener triggered with uri: $uri")
                if (uri != null && uri.scheme == "com.gokova.myanimelist") {
                    val code = uri.getQueryParameter("code")
                    android.util.Log.d("AuthScreen", "Intercepted code in listener: $code")
                    if (code != null) {
                        viewModel.handleAuthorizationCode(code)
                        intent.data = null
                    }
                }
            }
        activity?.addOnNewIntentListener(listener)
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

    AuthScreenContent(
        uiState = uiState,
        onLoginClick = {
            val intent = Intent(Intent.ACTION_VIEW, viewModel.generateAuthUrl())
            context.startActivity(intent)
        },
    )
}

@Composable
private fun AuthScreenContent(
    uiState: AuthUiState,
    onLoginClick: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AuthHeader()
            Spacer(modifier = Modifier.height(64.dp))
            AuthActionArea(
                uiState = uiState,
                onLoginClick = onLoginClick,
            )
        }
    }
}

@Composable
private fun AuthHeader() {
    Text(
        text = stringResource(id = R.string.feature_auth_app_title),
        style =
            MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            ),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(id = R.string.feature_auth_app_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun AuthActionArea(
    uiState: AuthUiState,
    onLoginClick: () -> Unit,
) {
    when (uiState) {
        is AuthUiState.Loading -> {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
            )
        }
        else -> {
            Button(
                onClick = onLoginClick,
                shape = MaterialTheme.shapes.large,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                modifier = Modifier.height(56.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.feature_auth_login_button),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

class AuthUiStatePreviewProvider : PreviewParameterProvider<AuthUiState> {
    override val values =
        sequenceOf(
            AuthUiState.Idle,
            AuthUiState.Loading,
            AuthUiState.Error("Security verification failed. Please try again."),
        )
}

@StandardPreviews
@Composable
internal fun AuthScreenPreview(
    @PreviewParameter(AuthUiStatePreviewProvider::class) state: AuthUiState,
) {
    MyAnimeListTheme {
        AuthScreenContent(
            uiState = state,
            onLoginClick = {},
        )
    }
}
