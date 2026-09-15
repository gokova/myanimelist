package com.gokova.myanimelist

import android.app.Application
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.logging.CrashlyticsTree
import com.gokova.myanimelist.logging.TimberLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyAnimeListApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initLogging()
        initCrashlytics()
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        AppLog.init { tag -> TimberLogger(tag) }
        AppLog.ui.i { "MyAnimeList application initialized (debug=${BuildConfig.DEBUG})" }
    }

    private fun initCrashlytics() {
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            val crashlytics = FirebaseCrashlytics.getInstance()
            val enableCrashlytics = !BuildConfig.DEBUG
            crashlytics.isCrashlyticsCollectionEnabled = enableCrashlytics
            if (enableCrashlytics) {
                Timber.plant(CrashlyticsTree(crashlytics))
            }
        }
    }
}
