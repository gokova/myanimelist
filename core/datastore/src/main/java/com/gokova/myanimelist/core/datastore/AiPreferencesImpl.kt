package com.gokova.myanimelist.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gokova.myanimelist.core.datastore.di.AiDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.subtle.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiPreferencesImpl(
    private val dataStore: DataStore<Preferences>,
    private val aeadProvider: () -> Aead,
) : AiPreferences {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @AiDataStore dataStore: DataStore<Preferences>,
    ) : this(
        dataStore = dataStore,
        aeadProvider = {
            try {
                AeadConfig.register()
            } catch (_: Exception) {
            }
            AndroidKeysetManager
                .Builder()
                .withSharedPref(context, "tink_keyset", "ai_prefs")
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://ai_master_key")
                .build()
                .keysetHandle
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        },
    )

    private val aead: Aead by lazy { aeadProvider() }

    private fun encrypt(data: String): String {
        val encrypted = aead.encrypt(data.toByteArray(Charsets.UTF_8), null)
        return Base64.encode(encrypted)
    }

    private fun decrypt(encryptedData: String): String? =
        try {
            val decoded = Base64.decode(encryptedData)
            String(aead.decrypt(decoded, null), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }

    override val geminiApiKey: Flow<String?> =
        dataStore.data.map { prefs ->
            prefs[KEY_GEMINI_API_KEY]?.let { decrypt(it) }
        }

    override suspend fun setGeminiApiKey(key: String) {
        val encrypted = encrypt(key.trim())
        dataStore.edit { prefs ->
            prefs[KEY_GEMINI_API_KEY] = encrypted
        }
    }

    override suspend fun clearGeminiApiKey() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_GEMINI_API_KEY)
        }
    }

    companion object {
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }
}
