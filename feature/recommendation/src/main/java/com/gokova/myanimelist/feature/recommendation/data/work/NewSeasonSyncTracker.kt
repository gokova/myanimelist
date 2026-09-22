package com.gokova.myanimelist.feature.recommendation.data.work

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface NewSeasonSyncTracker {
    fun getLastProcessedUserAnimeId(scopeKey: String = DEFAULT_SCOPE): Long

    fun setLastProcessedUserAnimeId(
        id: Long,
        scopeKey: String = DEFAULT_SCOPE,
    )

    fun getSyncStartTime(scopeKey: String = DEFAULT_SCOPE): Long

    fun setSyncStartTime(
        timestamp: Long,
        scopeKey: String = DEFAULT_SCOPE,
    )

    fun reset(scopeKey: String = DEFAULT_SCOPE)

    companion object {
        const val DEFAULT_SCOPE = "default"
        const val SCOPE_MANUAL = "manual"
        const val SCOPE_PERIODIC = "periodic"
    }
}

@Singleton
class NewSeasonSyncTrackerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : NewSeasonSyncTracker {
        private val prefs by lazy {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        override fun getLastProcessedUserAnimeId(scopeKey: String): Long =
            prefs.getLong(key(scopeKey, KEY_LAST_PROCESSED_ID), -1L)

        override fun setLastProcessedUserAnimeId(
            id: Long,
            scopeKey: String,
        ) {
            prefs.edit { putLong(key(scopeKey, KEY_LAST_PROCESSED_ID), id) }
        }

        override fun getSyncStartTime(scopeKey: String): Long =
            prefs.getLong(key(scopeKey, KEY_SYNC_START_TIME), 0L)

        override fun setSyncStartTime(
            timestamp: Long,
            scopeKey: String,
        ) {
            prefs.edit { putLong(key(scopeKey, KEY_SYNC_START_TIME), timestamp) }
        }

        override fun reset(scopeKey: String) {
            prefs.edit {
                remove(key(scopeKey, KEY_LAST_PROCESSED_ID))
                remove(key(scopeKey, KEY_SYNC_START_TIME))
            }
        }

        private fun key(
            scopeKey: String,
            suffix: String,
        ): String = "${scopeKey}_$suffix"

        companion object {
            private const val PREFS_NAME = "new_season_worker_prefs"
            private const val KEY_LAST_PROCESSED_ID = "last_processed_user_anime_id"
            private const val KEY_SYNC_START_TIME = "sync_start_time"
        }
    }
