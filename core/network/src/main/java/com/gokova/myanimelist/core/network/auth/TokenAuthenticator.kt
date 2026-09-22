package com.gokova.myanimelist.core.network.auth

import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.network.config.OAuthConfig
import com.gokova.myanimelist.core.network.di.Unauthenticated
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject

/**
 * Synchronous OkHttp [Authenticator] intercepting HTTP 401 responses for token refresh.
 *
 * NOTE ON SYNCHRONOUS INFRASTRUCTURE CONSTRAINT:
 * The OkHttp [Authenticator.authenticate] contract is strictly synchronous and runs on
 * background dispatcher threads. Synchronous token access ([AuthPreferences.getAccessTokenSync],
 * [AuthPreferences.getRefreshTokenSync]) and [runBlocking] for saving refreshed tokens are
 * intentional infrastructure requirements mandated by OkHttp. In-memory [AtomicReference]
 * caching is used to ensure O(1) non-blocking access during request execution.
 */
class TokenAuthenticator
    @Inject
    constructor(
        private val authPreferences: AuthPreferences,
        @Unauthenticated private val client: OkHttpClient,
        private val oAuthConfig: OAuthConfig,
    ) : Authenticator {
        private val json = Json { ignoreUnknownKeys = true }

        private sealed interface RefreshResult {
            data class Success(
                val tokenResponse: TokenResponse,
            ) : RefreshResult

            data object InvalidSession : RefreshResult

            data object TransientFailure : RefreshResult
        }

        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            if (shouldSkipAuthentication(response)) {
                return null
            }

            return synchronized(this) {
                resolveAuthenticatedRequest(response)
            }
        }

        private fun shouldSkipAuthentication(response: Response): Boolean =
            response.request.url.encodedPath
                .contains(TOKEN_ENDPOINT_PATH) ||
                responseCount(response) >= MAX_RETRY_COUNT

        private fun resolveAuthenticatedRequest(response: Response): Request? {
            val currentAccessToken = authPreferences.getAccessTokenSync()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (currentAccessToken != null && currentAccessToken != requestToken) {
                return response.request
                    .newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            return performTokenRefresh(response)
        }

        private fun performTokenRefresh(response: Response): Request? {
            val refreshToken = authPreferences.getRefreshTokenSync()
            if (refreshToken == null) {
                runBlocking {
                    authPreferences.clearTokens()
                }
                return null
            }

            return when (val result = refreshAccessToken(refreshToken)) {
                is RefreshResult.Success -> {
                    val newTokenResponse = result.tokenResponse
                    runBlocking {
                        authPreferences.saveTokens(
                            accessToken = newTokenResponse.accessToken,
                            refreshToken = newTokenResponse.refreshToken,
                        )
                    }
                    response.request
                        .newBuilder()
                        .header("Authorization", "Bearer ${newTokenResponse.accessToken}")
                        .build()
                }
                is RefreshResult.InvalidSession -> {
                    AppLog.data.w { "Token refresh returned invalid session; clearing credentials" }
                    runBlocking {
                        authPreferences.clearTokens()
                    }
                    null
                }
                is RefreshResult.TransientFailure -> {
                    AppLog.data.w { "Token refresh encountered transient error; preserving credentials" }
                    null
                }
            }
        }

        private fun refreshAccessToken(refreshToken: String): RefreshResult =
            try {
                val request = buildRefreshRequest(refreshToken)
                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> {
                            val bodyString = response.body.string()
                            val tokenResponse = json.decodeFromString<TokenResponse>(bodyString)
                            RefreshResult.Success(tokenResponse)
                        }
                        response.code in HTTP_AUTH_ERROR_RANGE -> {
                            AppLog.data.w { "Token refresh failed with auth error: ${response.code}" }
                            RefreshResult.InvalidSession
                        }
                        else -> {
                            AppLog.data.w { "Token refresh failed with server status: ${response.code}" }
                            RefreshResult.TransientFailure
                        }
                    }
                }
            } catch (e: IOException) {
                AppLog.data.w(e) { "Token refresh failed due to network I/O error" }
                RefreshResult.TransientFailure
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                AppLog.data.e(e) { "Token refresh encountered unexpected error" }
                RefreshResult.TransientFailure
            }

        private fun buildRefreshRequest(refreshToken: String): Request {
            val formBody =
                FormBody
                    .Builder()
                    .add("client_id", oAuthConfig.clientId)
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .build()

            return Request
                .Builder()
                .url(oAuthConfig.tokenUrl)
                .post(formBody)
                .build()
        }

        private fun responseCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }

        private companion object {
            private const val MAX_RETRY_COUNT = 3
            private const val TOKEN_ENDPOINT_PATH = "/oauth2/token"
            private val HTTP_AUTH_ERROR_RANGE = 400..401
        }
    }
