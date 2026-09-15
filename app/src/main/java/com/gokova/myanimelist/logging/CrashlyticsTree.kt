package com.gokova.myanimelist.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import java.util.concurrent.CancellationException

/**
 * Timber [Tree] implementation that routes logs and non-fatal exceptions to Firebase Crashlytics.
 */
class CrashlyticsTree(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
) : Timber.Tree() {
    override fun isLoggable(
        tag: String?,
        priority: Int,
    ): Boolean {
        // Only forward INFO, WARN, and ERROR levels to Crashlytics
        return priority >= Log.INFO
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        val tagPrefix = tag?.let { "[$it] " }.orEmpty()
        crashlytics.log("$tagPrefix$message")

        if (t != null && t !is CancellationException && priority >= Log.WARN) {
            crashlytics.recordException(t)
        }
    }
}
