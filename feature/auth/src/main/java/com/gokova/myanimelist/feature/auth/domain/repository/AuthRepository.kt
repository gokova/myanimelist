package com.gokova.myanimelist.feature.auth.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>

    /**
     * Generates the OAuth2 Authorization URL and persists the temporary PKCE code verifier.
     * Returns the complete URL string (platform-neutral).
     */
    suspend fun getAuthorizationUrl(): String

    /**
     * Validates the OAuth state and exchanges the authorization code for access and refresh tokens.
     */
    suspend fun authenticate(
        code: String,
        state: String? = null,
    ): Result<Unit>

    /**
     * Clears user session and stored tokens.
     */
    suspend fun logout()
}
