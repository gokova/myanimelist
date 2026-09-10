package com.gokova.myanimelist.feature.auth.data

import com.gokova.myanimelist.core.network.auth.TokenResponse
import com.gokova.myanimelist.core.network.config.OAuthConfig
import com.gokova.myanimelist.core.network.di.Unauthenticated
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

/**
 * Remote client responsible exclusively for OAuth network calls with MyAnimeList.
 */
class MalOAuthClient
    @Inject
    constructor(
        @Unauthenticated private val client: OkHttpClient,
        private val oAuthConfig: OAuthConfig,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        suspend fun exchangeCodeForTokens(
            code: String,
            codeVerifier: String,
        ): Result<TokenResponse> =
            withContext(Dispatchers.IO) {
                try {
                    val formBody =
                        FormBody
                            .Builder()
                            .add("client_id", oAuthConfig.clientId)
                            .add("grant_type", "authorization_code")
                            .add("code", code)
                            .add("code_verifier", codeVerifier)
                            .build()

                    val request =
                        Request
                            .Builder()
                            .url(oAuthConfig.tokenUrl)
                            .post(formBody)
                            .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body.string()
                            val tokenResponse = json.decodeFromString<TokenResponse>(bodyString)
                            Result.success(tokenResponse)
                        } else {
                            val errorBody = response.body.string()
                            Result.failure(Exception("Failed to exchange token: ${response.code} $errorBody"))
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    Result.failure(e)
                }
            }
    }
