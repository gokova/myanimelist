package com.gokova.myanimelist.core.network.config

/**
 * Encapsulates OAuth2 configuration parameters for MyAnimeList API.
 */
data class OAuthConfig(
    val clientId: String,
    val authorizeUrl: String = DEFAULT_AUTHORIZE_URL,
    val tokenUrl: String = DEFAULT_TOKEN_URL,
    val redirectUri: String = DEFAULT_REDIRECT_URI,
) {
    companion object {
        const val DEFAULT_AUTHORIZE_URL = "https://myanimelist.net/v1/oauth2/authorize"
        const val DEFAULT_TOKEN_URL = "https://myanimelist.net/v1/oauth2/token"
        const val DEFAULT_REDIRECT_URI = "com.gokova.myanimelist://oauth2redirect"
    }
}
