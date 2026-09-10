package com.gokova.myanimelist.feature.auth

/**
 * Represents the parsed result of an OAuth redirect callback.
 */
sealed interface OAuthRedirectResult {
    /**
     * Successful authorization with authorization code and optional CSRF state.
     */
    data class Success(
        val code: String,
        val state: String? = null,
    ) : OAuthRedirectResult

    /**
     * Authorization error or user cancellation (e.g. access_denied).
     */
    data class Error(
        val error: String,
        val description: String? = null,
    ) : OAuthRedirectResult
}
