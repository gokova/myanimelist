package com.gokova.myanimelist.core.datastore

import kotlinx.coroutines.flow.Flow

interface AuthPreferences {
    val accessToken: Flow<String?>
    val refreshToken: Flow<String?>
    val isLoggedIn: Flow<Boolean>
    val codeVerifier: Flow<String?>
    val oauthState: Flow<String?>

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
    )

    suspend fun saveCodeVerifier(verifier: String)

    suspend fun clearCodeVerifier()

    suspend fun saveOAuthState(state: String)

    suspend fun clearOAuthState()

    suspend fun clearTokens()

    suspend fun warmCache()

    fun getAccessTokenSync(): String?

    fun getRefreshTokenSync(): String?
}
