package com.gokova.myanimelist.core.network.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val HTTP_TOO_MANY_REQUESTS = 429
private val HTTP_SERVER_ERROR_RANGE = 500..599
val DEFAULT_INITIAL_RETRY_DELAY: Duration = 500.milliseconds
const val DEFAULT_BACKOFF_MULTIPLIER = 2
const val DEFAULT_MAX_RETRY_ATTEMPTS = 3

/**
 * Executes a network [block] with exponential backoff retry for transient network errors
 * ([IOException], HTTP 429 Too Many Requests, and HTTP 500..599 server errors).
 *
 * Rethrows immediately on non-retryable HTTP errors (e.g. 401, 403, 404) or [CancellationException].
 */
suspend fun <T> executeWithRetry(
    maxAttempts: Int = DEFAULT_MAX_RETRY_ATTEMPTS,
    initialDelay: Duration = DEFAULT_INITIAL_RETRY_DELAY,
    backoffMultiplier: Int = DEFAULT_BACKOFF_MULTIPLIER,
    block: suspend () -> T,
): T {
    var currentAttempt = 0
    var delayDuration = initialDelay

    while (true) {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            currentAttempt++
            if (!isRetryableNetworkError(e) || currentAttempt >= maxAttempts) {
                throw e
            }
            delay(delayDuration)
            delayDuration *= backoffMultiplier
        }
    }
}

/**
 * Checks whether the given [throwable] is a transient/retryable network error.
 */
fun isRetryableNetworkError(throwable: Throwable): Boolean =
    when (throwable) {
        is IOException -> true
        is HttpException ->
            throwable.code() == HTTP_TOO_MANY_REQUESTS ||
                throwable.code() in HTTP_SERVER_ERROR_RANGE
        else -> false
    }
