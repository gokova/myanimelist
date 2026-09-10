package com.gokova.myanimelist.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_prefs")

@Singleton
class SyncPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SyncPreferences {
        private val lastSyncKey = longPreferencesKey("last_anime_list_sync_timestamp")

        override val lastAnimeListSyncTimestamp: Flow<Long> =
            context.syncDataStore.data.map { preferences ->
                preferences[lastSyncKey] ?: 0L
            }

        override suspend fun getLastAnimeListSyncTimestamp(): Long = lastAnimeListSyncTimestamp.first()

        override suspend fun updateLastAnimeListSyncTimestamp(timestamp: Long) {
            context.syncDataStore.edit { preferences ->
                preferences[lastSyncKey] = timestamp
            }
        }

        override suspend fun clearSyncPreferences() {
            context.syncDataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }
