package com.gokova.myanimelist.core.datastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AuthPreferences {
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

        private fun decrypt(data: String): String? =
            try {
                val decoded = Base64.decode(data, Base64.NO_WRAP)
                val decrypted = aead.decrypt(decoded, null)
                String(decrypted, Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }

        private fun getDecryptedFlow(key: Preferences.Key<String>): Flow<String?> =
            context.dataStore.data.map { prefs ->
                prefs[key]?.let { decrypt(it) }
            }

        override val accessToken: Flow<String?> = getDecryptedFlow(KEY_ACCESS_TOKEN)
        override val refreshToken: Flow<String?> = getDecryptedFlow(KEY_REFRESH_TOKEN)
        override val isLoggedIn: Flow<Boolean> = accessToken.map { it != null }
        override val codeVerifier: Flow<String?> = getDecryptedFlow(KEY_CODE_VERIFIER)

        override suspend fun saveTokens(
            accessToken: String,
            refreshToken: String,
        ) {
            context.dataStore.edit { prefs ->
                prefs[KEY_ACCESS_TOKEN] = encrypt(accessToken)
                prefs[KEY_REFRESH_TOKEN] = encrypt(refreshToken)
            }
        }

        override suspend fun saveCodeVerifier(verifier: String) {
            context.dataStore.edit { prefs ->
                prefs[KEY_CODE_VERIFIER] = encrypt(verifier)
            }
        }

        override suspend fun clearCodeVerifier() {
            context.dataStore.edit { prefs ->
                prefs.remove(KEY_CODE_VERIFIER)
            }
        }

        override suspend fun clearTokens() {
            context.dataStore.edit { prefs ->
                prefs.remove(KEY_ACCESS_TOKEN)
                prefs.remove(KEY_REFRESH_TOKEN)
            }
        }

        override fun getAccessTokenSync(): String? = runBlocking { accessToken.first() }

        override fun getRefreshTokenSync(): String? = runBlocking { refreshToken.first() }

        companion object {
            private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
            private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
            private val KEY_CODE_VERIFIER = stringPreferencesKey("code_verifier")
        }
    }
