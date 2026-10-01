package com.gokova.myanimelist.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gokova.myanimelist.core.domain.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDataStoreTest {
    @Test
    fun `clearing session data removes profile and token keys atomically`() =
        runTest {
            val dataStore = InMemoryPreferencesDataStore()
            val userPreferences = UserPreferencesImpl(dataStore)

            // Seed token keys in the same store
            val tokenKey = stringPreferencesKey("access_token")
            dataStore.edit { prefs ->
                prefs[tokenKey] = "encrypted_token"
            }

            // Seed user profile
            val profile =
                UserProfile(
                    id = 12345L,
                    name = "TestUser",
                    pictureUrl = "https://example.com/avatar.jpg",
                )
            val saved = userPreferences.saveUserProfile(profile)
            assertTrue(saved)

            // Verify both profile and token exist
            assertEquals("TestUser", userPreferences.userProfile.first()?.name)
            assertEquals("encrypted_token", dataStore.data.first()[tokenKey])

            // Atomically clear entire session store
            dataStore.edit { it.clear() }

            // Verify both profile and tokens are cleared in the single store
            assertNull(userPreferences.userProfile.first())
            assertNull(dataStore.data.first()[tokenKey])
        }

    @Test
    fun `clearUserProfile only clears profile keys leaving other session keys intact`() =
        runTest {
            val dataStore = InMemoryPreferencesDataStore()
            val userPreferences = UserPreferencesImpl(dataStore)
            val tokenKey = stringPreferencesKey("access_token")
            dataStore.edit { it[tokenKey] = "keep_me" }

            val profile = UserProfile(id = 99L, name = "KeepTokenUser")
            val saved = userPreferences.saveUserProfile(profile)
            assertTrue(saved)

            userPreferences.clearUserProfile()

            assertNull(userPreferences.userProfile.first())
            assertEquals("keep_me", dataStore.data.first()[tokenKey])
        }

    @Test
    fun `saveUserProfile rejects write when session is not active`() =
        runTest {
            val dataStore = InMemoryPreferencesDataStore()
            val userPreferences = UserPreferencesImpl(dataStore)
            val profile = UserProfile(id = 123L, name = "UnauthenticatedUser")

            val saved = userPreferences.saveUserProfile(profile)

            assertFalse(saved)
            assertNull(userPreferences.userProfile.first())
        }

    @Test
    fun `saveUserProfile rejects write when sessionId does not match expectedSessionId`() =
        runTest {
            val dataStore = InMemoryPreferencesDataStore()
            val userPreferences = UserPreferencesImpl(dataStore)
            val tokenKey = stringPreferencesKey("access_token")
            val sessionKey = stringPreferencesKey("session_id")
            dataStore.edit { prefs ->
                prefs[tokenKey] = "encrypted_token"
                prefs[sessionKey] = "new_session_id"
            }

            val profile = UserProfile(id = 123L, name = "User")
            val saved =
                userPreferences.saveUserProfile(
                    profile = profile,
                    expectedSessionId = "old_session_id",
                )

            assertFalse(saved)
            assertNull(userPreferences.userProfile.first())
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
