package com.gokova.myanimelist.feature.recommendation.presentation

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface BackgroundSyncPermissionManager {
    fun isIgnoringBatteryOptimizations(): Boolean

    fun shouldPrompt(): Boolean

    fun markPrompted()

    fun createPermissionIntent(): Intent
}

@Singleton
class BackgroundSyncPermissionManagerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BackgroundSyncPermissionManager {
        private val prefs by lazy {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        override fun isIgnoringBatteryOptimizations(): Boolean {
            val powerManager =
                context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }

        override fun shouldPrompt(): Boolean {
            val alreadyPrompted = prefs.getBoolean(KEY_HAS_BEEN_PROMPTED, false)
            return !alreadyPrompted && !isIgnoringBatteryOptimizations()
        }

        override fun markPrompted() {
            prefs.edit { putBoolean(KEY_HAS_BEEN_PROMPTED, true) }
        }

        override fun createPermissionIntent(): Intent =
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

        companion object {
            private const val PREFS_NAME = "background_sync_prefs"
            private const val KEY_HAS_BEEN_PROMPTED = "has_been_prompted"
        }
    }
