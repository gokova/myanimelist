package com.gokova.myanimelist.logging

import com.gokova.myanimelist.core.domain.logging.Logger
import timber.log.Timber

/**
 * Android [Logger] implementation delegating to [Timber] with an explicit tag.
 */
class TimberLogger(
    private val tag: String,
) : Logger {
    override fun v(message: () -> String) {
        Timber.tag(tag).v(message())
    }

    override fun d(message: () -> String) {
        Timber.tag(tag).d(message())
    }

    override fun i(message: () -> String) {
        Timber.tag(tag).i(message())
    }

    override fun w(
        throwable: Throwable?,
        message: () -> String,
    ) {
        if (throwable != null) {
            Timber.tag(tag).w(throwable, message())
        } else {
            Timber.tag(tag).w(message())
        }
    }

    override fun e(
        throwable: Throwable?,
        message: () -> String,
    ) {
        if (throwable != null) {
            Timber.tag(tag).e(throwable, message())
        } else {
            Timber.tag(tag).e(message())
        }
    }
}
