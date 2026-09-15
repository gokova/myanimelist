package com.gokova.myanimelist.core.domain.logging

/**
 * Pure Kotlin Logger contract with zero Android framework dependencies.
 */
interface Logger {
    fun v(message: () -> String)

    fun d(message: () -> String)

    fun i(message: () -> String)

    fun w(
        throwable: Throwable? = null,
        message: () -> String,
    )

    fun e(
        throwable: Throwable? = null,
        message: () -> String,
    )
}

/**
 * No-Op implementation of [Logger] used as the default or in release/test builds.
 */
object NoOpLogger : Logger {
    override fun v(message: () -> String) = Unit

    override fun d(message: () -> String) = Unit

    override fun i(message: () -> String) = Unit

    override fun w(
        throwable: Throwable?,
        message: () -> String,
    ) = Unit

    override fun e(
        throwable: Throwable?,
        message: () -> String,
    ) = Unit
}
