package com.gokova.myanimelist.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiPreferencesTest {
    private val fakeAead =
        object : Aead {
            override fun encrypt(
                plaintext: ByteArray?,
                associatedData: ByteArray?,
            ): ByteArray {
                val data = plaintext ?: ByteArray(0)
                return data.map { (it + 1).toByte() }.toByteArray()
            }

            override fun decrypt(
                ciphertext: ByteArray?,
                associatedData: ByteArray?,
            ): ByteArray {
                val data = ciphertext ?: ByteArray(0)
                return data.map { (it - 1).toByte() }.toByteArray()
            }
        }

    @Test
    fun `setGeminiApiKey encrypts and emits decrypted value`() =
        runTest {
            val dataStore = InMemoryPreferencesDataStore()
            val aiPreferences = AiPreferencesImpl(dataStore) { fakeAead }

            aiPreferences.setGeminiApiKey("AIzaSyFakeKey123")

            val rawPref = dataStore.data.first()[AiPreferencesImpl.KEY_GEMINI_API_KEY]
            assertNotEquals("AIzaSyFakeKey123", rawPref)
            assertEquals("AIzaSyFakeKey123", aiPreferences.geminiApiKey.first())
        }

    @Test
    fun `clearGeminiApiKey removes stored key`() =
        runTest {
            val dataStore = InMemoryPreferencesDataStore()
            val aiPreferences = AiPreferencesImpl(dataStore) { fakeAead }

            aiPreferences.setGeminiApiKey("AIzaSyFakeKey123")
            assertEquals("AIzaSyFakeKey123", aiPreferences.geminiApiKey.first())

            aiPreferences.clearGeminiApiKey()
            assertNull(aiPreferences.geminiApiKey.first())
        }

    @Test
    fun `clearing session store does not clear ai store`() =
        runTest {
            val sessionStore = InMemoryPreferencesDataStore()
            val aiStore = InMemoryPreferencesDataStore()
            val aiPreferences = AiPreferencesImpl(aiStore) { fakeAead }

            val sessionTokenKey = stringPreferencesKey("access_token")
            sessionStore.updateData { prefs ->
                val mutable = prefs.toMutablePreferences()
                mutable[sessionTokenKey] = "tok"
                mutable
            }
            aiPreferences.setGeminiApiKey("AIzaSyKeepThisKey")

            // Simulate MAL logout clearing session store
            sessionStore.updateData { emptyPreferences() }

            assertNull(sessionStore.data.first()[sessionTokenKey])
            assertEquals("AIzaSyKeepThisKey", aiPreferences.geminiApiKey.first())
        }

    private class InMemoryPreferencesDataStore(
        initialPreferences: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val _data = MutableStateFlow(initialPreferences)
        override val data: Flow<Preferences> = _data.asStateFlow()

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            val updated = transform(_data.value)
            _data.value = updated
            return updated
        }
    }
}
