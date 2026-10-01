package com.gokova.myanimelist.core.datastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gokova.myanimelist.core.datastore.di.SessionDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class AuthPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @SessionDataStore private val dataStore: DataStore<Preferences>,
    ) : AuthPreferences {
        private val cachedAccessToken = AtomicReference<String?>(null)
        private val cachedRefreshToken = AtomicReference<String?>(null)

        init {
            try {
                AeadConfig.register()
            } catch (_: Exception) {
            }
        }

        private val aead: Aead by lazy {
            AndroidKeysetManager
                .Builder()
                .withSharedPref(context, "tink_keyset", "auth_prefs")
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://auth_master_key")
                .build()
                .keysetHandle
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        }

        private fun encrypt(data: String): String {
            val encrypted = aead.encrypt(data.toByteArray(Charsets.UTF_8), null)
            return Base64.encodeToString(encrypted, Base64.NO_WRAP)
        }

        private fun decrypt(encryptedData: String): String? =
            try {
                val decoded = Base64.decode(encryptedData, Base64.NO_WRAP)
                String(aead.decrypt(decoded, null), Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }

        private fun getDecryptedFlow(key: Preferences.Key<String>): Flow<String?> =
            dataStore.data.map { prefs ->
                prefs[key]?.let { decrypt(it) }
            }

        override val accessToken: Flow<String?> =
            getDecryptedFlow(KEY_ACCESS_TOKEN).onEach {
                cachedAccessToken.set(it)
            }

        override val refreshToken: Flow<String?> =
            getDecryptedFlow(KEY_REFRESH_TOKEN).onEach {
                cachedRefreshToken.set(it)
            }

        override val isLoggedIn: Flow<Boolean> = accessToken.map { it != null }

        override val codeVerifier: Flow<String?> = getDecryptedFlow(KEY_CODE_VERIFIER)

        override val oauthState: Flow<String?> = getDecryptedFlow(KEY_OAUTH_STATE)

        override val sessionId: Flow<String?> =
            dataStore.data.map { prefs ->
                if (prefs[KEY_ACCESS_TOKEN] != null) {
                    prefs[KEY_SESSION_ID] ?: "legacy_session"
                } else {
                    null
                }
            }

        override suspend fun saveTokens(
            accessToken: String,
            refreshToken: String,
            isNewSession: Boolean,
        ) {
            val encryptedAccess = encrypt(accessToken)
            val encryptedRefresh = encrypt(refreshToken)
            dataStore.edit { prefs ->
                prefs[KEY_ACCESS_TOKEN] = encryptedAccess
                prefs[KEY_REFRESH_TOKEN] = encryptedRefresh
                if (isNewSession || prefs[KEY_SESSION_ID] == null) {
                    prefs[KEY_SESSION_ID] = UUID.randomUUID().toString()
                }
                // Atomically clean up temporary OAuth PKCE verifier and CSRF state in the same transaction
                prefs.remove(KEY_CODE_VERIFIER)
                prefs.remove(KEY_OAUTH_STATE)
            }
            // Update in-memory cache only AFTER DataStore write successfully completes
            cachedAccessToken.set(accessToken)
            cachedRefreshToken.set(refreshToken)
        }

        override suspend fun clearTokens() {
            dataStore.edit { prefs ->
                prefs.clear()
            }
            // Clear in-memory cache only AFTER DataStore removal successfully completes
            cachedAccessToken.set(null)
            cachedRefreshToken.set(null)
        }

        override suspend fun saveOAuthSession(
            verifier: String,
            state: String,
        ) {
            val encryptedVerifier = encrypt(verifier)
            val encryptedState = encrypt(state)
            dataStore.edit { prefs ->
                prefs[KEY_CODE_VERIFIER] = encryptedVerifier
                prefs[KEY_OAUTH_STATE] = encryptedState
            }
        }

        override suspend fun clearOAuthSession() {
            dataStore.edit { prefs ->
                prefs.remove(KEY_CODE_VERIFIER)
                prefs.remove(KEY_OAUTH_STATE)
            }
        }

        override suspend fun warmCache() {
            val token = accessToken.first()
            cachedAccessToken.set(token)
            val refresh = refreshToken.first()
            cachedRefreshToken.set(refresh)
        }

        /**
         * Returns the access token synchronously.
         *
         * Concurrency & Cache Strategy:
         * Uses an [AtomicReference] in-memory cache for O(1) non-blocking reads during OkHttp request interception.
         * Falls back to a synchronous read only if the cache is cold on initial app boot before [warmCache] finishes.
         * Note: Must only be called from background dispatcher threads (e.g. OkHttp interceptor/authenticator).
         */
        override fun getAccessTokenSync(): String? {
            cachedAccessToken.get()?.let { return it }
            return runBlocking {
                accessToken.first()?.also {
                    cachedAccessToken.set(it)
                }
            }
        }

        /**
         * Returns the refresh token synchronously with the same in-memory cache strategy as [getAccessTokenSync].
         */
        override fun getRefreshTokenSync(): String? {
            cachedRefreshToken.get()?.let { return it }
            return runBlocking {
                refreshToken.first()?.also {
                    cachedRefreshToken.set(it)
                }
            }
        }

        companion object {
            internal val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
            internal val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
            internal val KEY_CODE_VERIFIER = stringPreferencesKey("code_verifier")
            internal val KEY_OAUTH_STATE = stringPreferencesKey("oauth_state")
            internal val KEY_SESSION_ID = stringPreferencesKey("session_id")
        }
    }
