package com.gokova.myanimelist.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import com.gokova.myanimelist.feature.auth.OAuthRedirectResult

/**
 * Manages observation, extraction, and one-time consumption of OAuth redirect results from incoming intents.
 */
class OAuthRedirectHandler(
    private val expectedScheme: String = EXPECTED_SCHEME,
    private val expectedHost: String = EXPECTED_HOST,
) {
    private val _redirectResult = mutableStateOf<OAuthRedirectResult?>(null)
    val redirectResult: State<OAuthRedirectResult?> = _redirectResult

    fun handleIntent(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        val handled =
            handleUri(
                scheme = uri.scheme,
                host = uri.host,
                getQueryParameter = uri::getQueryParameter,
            )
        if (handled) {
            intent.data = null
        }
        return handled
    }

    fun handleUri(
        scheme: String?,
        host: String?,
        getQueryParameter: (String) -> String?,
    ): Boolean {
        val result =
            extractOAuthResult(
                scheme = scheme,
                host = host,
                getQueryParameter = getQueryParameter,
                expectedScheme = expectedScheme,
                expectedHost = expectedHost,
            ) ?: return false
        _redirectResult.value = result
        return true
    }

    fun consumeResult() {
        _redirectResult.value = null
    }

    companion object {
        const val EXPECTED_SCHEME = "com.gokova.myanimelist"
        const val EXPECTED_HOST = "oauth2redirect"
    }
}

@Composable
fun rememberOAuthRedirectHandler(): OAuthRedirectHandler {
    val handler = remember { OAuthRedirectHandler() }
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    LaunchedEffect(activity?.intent) {
        handler.handleIntent(activity?.intent)
    }

    DisposableEffect(activity) {
        val listener =
            Consumer<Intent> { intent ->
                handler.handleIntent(intent)
            }
        activity?.addOnNewIntentListener(listener)
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

    return handler
}

internal fun extractOAuthResult(
    scheme: String?,
    host: String?,
    getQueryParameter: (String) -> String?,
    expectedScheme: String = OAuthRedirectHandler.EXPECTED_SCHEME,
    expectedHost: String = OAuthRedirectHandler.EXPECTED_HOST,
): OAuthRedirectResult? {
    if (scheme != expectedScheme || host != expectedHost) {
        return null
    }

    val error = getQueryParameter("error")
    val code = getQueryParameter("code")

    return when {
        error != null ->
            OAuthRedirectResult.Error(
                error = error,
                description = getQueryParameter("error_description"),
            )
        code != null ->
            OAuthRedirectResult.Success(
                code = code,
                state = getQueryParameter("state"),
            )
        else -> null
    }
}
