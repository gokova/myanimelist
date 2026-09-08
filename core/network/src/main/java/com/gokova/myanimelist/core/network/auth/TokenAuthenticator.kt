package com.gokova.myanimelist.core.network.auth

import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.core.network.BuildConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator
    @Inject
    constructor(
        private val authPreferences: AuthPreferences,
    ) : Authenticator {
        private val json = Json { ignoreUnknownKeys = true }

        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            var newRequest: Request? = null

            // If the request itself was a token refresh, stop to avoid infinite loop
            if (!response.request.url.encodedPath
                    .contains("/oauth2/token")
            ) {
                val refreshToken = authPreferences.getRefreshTokenSync()

                if (refreshToken != null) {
                    val newTokenResponse = refreshAccessToken(refreshToken)

                    if (newTokenResponse != null) {
                        runBlocking {
                            authPreferences.saveTokens(
                                accessToken = newTokenResponse.accessToken,
                                refreshToken = newTokenResponse.refreshToken,
                            )
                        }
                        newRequest =
                            response.request
                                .newBuilder()
                                .header("Authorization", "Bearer ${newTokenResponse.accessToken}")
                                .build()
                    } else {
                        // Refresh failed, clear tokens -> causes isLoggedIn to emit false
                        runBlocking {
                            authPreferences.clearTokens()
                        }
                    }
                }
            }

            return newRequest
        }

        private fun refreshAccessToken(refreshToken: String): TokenResponse? {
            val client = OkHttpClient()

            val formBody =
                FormBody
                    .Builder()
                    .add("client_id", BuildConfig.MAL_CLIENT_ID)
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .build()

            val request =
                Request
                    .Builder()
                    .url("https://myanimelist.net/v1/oauth2/token")
                    .post(formBody)
                    .build()

            return try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body.string().let { bodyString ->
                        json.decodeFromString<TokenResponse>(bodyString)
                    }
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
