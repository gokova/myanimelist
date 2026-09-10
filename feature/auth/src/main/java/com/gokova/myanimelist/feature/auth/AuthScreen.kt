package com.gokova.myanimelist.feature.auth

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme

@Composable
fun AuthScreen(
    onNavigateToOAuthUrl: (String) -> Unit,
    onAuthSuccess: () -> Unit,
    redirectResult: OAuthRedirectResult? = null,
    onRedirectResultConsumed: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(redirectResult) {
        val result = redirectResult ?: return@LaunchedEffect
        onRedirectResultConsumed()
        when (result) {
            is OAuthRedirectResult.Success -> {
                viewModel.handleAuthorizationCode(result.code, result.state)
            }
            is OAuthRedirectResult.Error -> {
                viewModel.handleRedirectError(result.error)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthUiEvent.OpenOAuthUrl -> {
                    onNavigateToOAuthUrl(event.url)
                }
                is AuthUiEvent.AuthSuccess -> {
                    onAuthSuccess()
                }
            }
        }
    }

    AuthScreenContent(
        uiState = uiState,
        onLoginClick = viewModel::onLoginClicked,
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
                    text = stringResource(id = uiState.messageResId),
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
            AuthUiState.Error(R.string.feature_auth_error_verification_failed),
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
