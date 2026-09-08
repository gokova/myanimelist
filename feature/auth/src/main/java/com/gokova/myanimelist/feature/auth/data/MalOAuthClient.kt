package com.gokova.myanimelist.feature.auth.data

import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.core.network.BuildConfig
import com.gokova.myanimelist.core.network.auth.TokenResponse
import com.gokova.myanimelist.feature.auth.domain.authenticator.MalAuthenticator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import javax.inject.Inject

class MalOAuthClient
    @Inject
    constructor(
        private val authPreferences: AuthPreferences,
        private val client: OkHttpClient,
    ) : MalAuthenticator {
        private val json = Json { ignoreUnknownKeys = true }

        override suspend fun getAuthorizationUrl(): Uri {
            val verifier = generateCodeVerifier()
            val challenge = generateCodeChallenge(verifier)

            authPreferences.saveCodeVerifier(verifier)

            return "https://myanimelist.net/v1/oauth2/authorize"
                .toUri()
                .buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", BuildConfig.MAL_CLIENT_ID)
                .appendQueryParameter("code_challenge", challenge)
                .appendQueryParameter("code_challenge_method", "plain")
                .build()
        }

        override suspend fun authenticate(code: String): Result<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    val verifier =
                        authPreferences.codeVerifier.first()
                            ?: return@withContext Result.failure(Exception("Missing code verifier"))

                    val formBody =
                        FormBody
                            .Builder()
                            .add("client_id", BuildConfig.MAL_CLIENT_ID)
                            .add("grant_type", "authorization_code")
                            .add("code", code)
                            .add("code_verifier", verifier)
                            .build()

                    val request =
                        Request
                            .Builder()
                            .url("https://myanimelist.net/v1/oauth2/token")
                            .post(formBody)
                            .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bodyString = response.body.string()
                        android.util.Log.d("MalOAuthClient", "Token exchange success: $bodyString")
                        val tokenResponse = json.decodeFromString<TokenResponse>(bodyString)

                        authPreferences.saveTokens(
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                        )
                        authPreferences.clearCodeVerifier()
                        Result.success(Unit)
                    } else {
                        val errorBody = response.body.string()
                        android.util.Log.e(
                            "MalOAuthClient",
                            "Failed to exchange token. Code: ${response.code}, Body: $errorBody",
                        )
                        Result.failure(Exception("Failed to exchange token: $errorBody"))
                    }
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    Result.failure(e)
                }
            }
        }

        override suspend fun logout() {
            authPreferences.clearTokens()
            authPreferences.clearCodeVerifier()
        }

        private fun generateCodeVerifier(): String {
            val secureRandom = SecureRandom()
            val bytes = ByteArray(96)
            secureRandom.nextBytes(bytes)
            return Base64
                .encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                .take(CODE_VERIFIER_MAX_LENGTH)
        }

        private fun generateCodeChallenge(verifier: String): String = verifier

        companion object {
            private const val CODE_VERIFIER_MAX_LENGTH = 128
        }
    }
