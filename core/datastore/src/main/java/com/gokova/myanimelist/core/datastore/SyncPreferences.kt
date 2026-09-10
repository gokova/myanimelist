package com.gokova.myanimelist.core.datastore

import kotlinx.coroutines.flow.Flow

interface SyncPreferences {
    val lastAnimeListSyncTimestamp: Flow<Long>

    suspend fun getLastAnimeListSyncTimestamp(): Long

    suspend fun updateLastAnimeListSyncTimestamp(timestamp: Long)

    suspend fun clearSyncPreferences()
}
