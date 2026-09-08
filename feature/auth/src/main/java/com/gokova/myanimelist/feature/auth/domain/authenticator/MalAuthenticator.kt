package com.gokova.myanimelist.feature.auth.domain.authenticator

import android.net.Uri

interface MalAuthenticator {
    /**
     * Generates the OAuth2 Authorization URL and saves the PKCE code verifier.
     */
    suspend fun getAuthorizationUrl(): Uri

    /**
     * Exchanges the authorization code for access and refresh tokens, and saves them.
     */
    suspend fun authenticate(code: String): Result<Unit>

    /**
     * Clears user session and stored tokens.
     */
    suspend fun logout()
}
