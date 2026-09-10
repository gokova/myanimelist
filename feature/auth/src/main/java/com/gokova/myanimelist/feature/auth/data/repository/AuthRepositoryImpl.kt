package com.gokova.myanimelist.feature.auth.data.repository

import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.core.network.config.OAuthConfig
import com.gokova.myanimelist.feature.auth.data.MalOAuthClient
import com.gokova.myanimelist.feature.auth.data.pkce.PkceGenerator
import com.gokova.myanimelist.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val authPreferences: AuthPreferences,
        private val pkceGenerator: PkceGenerator,
        private val oAuthClient: MalOAuthClient,
        private val oAuthConfig: OAuthConfig,
    ) : AuthRepository {
        override val isLoggedIn: Flow<Boolean> = authPreferences.isLoggedIn

        override suspend fun getAuthorizationUrl(): String {
            val verifier = pkceGenerator.generateCodeVerifier()
            val challenge = pkceGenerator.generateCodeChallenge(verifier)
            val state = pkceGenerator.generateState()

            authPreferences.saveCodeVerifier(verifier)
            authPreferences.saveOAuthState(state)

            return oAuthConfig.authorizeUrl
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("response_type", "code")
                .addQueryParameter("client_id", oAuthConfig.clientId)
                .addQueryParameter("code_challenge", challenge)
                .addQueryParameter("code_challenge_method", "plain")
                .addQueryParameter("state", state)
                .build()
                .toString()
        }

        override suspend fun authenticate(
            code: String,
            state: String?,
        ): Result<Unit> {
            val savedState = authPreferences.oauthState.first()
            if (savedState != null && savedState != state) {
                authPreferences.clearOAuthState()
                authPreferences.clearCodeVerifier()
                return Result.failure(SecurityException("OAuth state mismatch / CSRF detected"))
            }

            val verifier = authPreferences.codeVerifier.first()
            return if (verifier != null) {
                val tokenResult = oAuthClient.exchangeCodeForTokens(code = code, codeVerifier = verifier)
                tokenResult.fold(
                    onSuccess = { tokenResponse ->
                        // saveTokens atomically commits tokens and removes transient verifier and state
                        authPreferences.saveTokens(
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                        )
                        Result.success(Unit)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    },
                )
            } else {
                Result.failure(IllegalStateException("Missing PKCE code verifier"))
            }
        }

        override suspend fun logout() {
            authPreferences.clearTokens()
        }
    }
