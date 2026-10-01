package com.gokova.myanimelist.core.datastore

import kotlinx.coroutines.flow.Flow

interface AuthPreferences {
    val accessToken: Flow<String?>
    val refreshToken: Flow<String?>
    val isLoggedIn: Flow<Boolean>
    val codeVerifier: Flow<String?>
    val oauthState: Flow<String?>
    val sessionId: Flow<String?>

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        isNewSession: Boolean = false,
    )

    suspend fun clearTokens()

    suspend fun saveOAuthSession(
        verifier: String,
        state: String,
    )

    suspend fun clearOAuthSession()

    suspend fun warmCache()

    fun getAccessTokenSync(): String?

    fun getRefreshTokenSync(): String?
}
