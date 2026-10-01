package com.gokova.myanimelist.core.network.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

class NetworkRetryTest {
    @Test
    fun `executeWithRetry returns result immediately on first attempt success`() =
        runTest {
            var calls = 0
            val result =
                executeWithRetry(initialDelay = 1.milliseconds) {
                    calls++
                    "success"
                }

            assertEquals("success", result)
            assertEquals(1, calls)
        }

    @Test
    fun `executeWithRetry retries on IOException and succeeds`() =
        runTest {
            var calls = 0
            val result =
                executeWithRetry(initialDelay = 1.milliseconds) {
                    calls++
                    if (calls < 2) throw IOException("Transient failure")
                    "recovered"
                }

            assertEquals("recovered", result)
            assertEquals(2, calls)
        }

    @Test
    fun `executeWithRetry retries on HTTP 429 and succeeds`() =
        runTest {
            var calls = 0
            val result =
                executeWithRetry(initialDelay = 1.milliseconds) {
                    calls++
                    if (calls < 2) throw createHttpException(429)
                    "rate_limit_recovered"
                }

            assertEquals("rate_limit_recovered", result)
            assertEquals(2, calls)
        }

    @Test
    fun `executeWithRetry retries on HTTP 500 server error and succeeds`() =
        runTest {
            var calls = 0
            val result =
                executeWithRetry(initialDelay = 1.milliseconds) {
                    calls++
                    if (calls < 2) throw createHttpException(500)
                    "server_recovered"
                }

            assertEquals("server_recovered", result)
            assertEquals(2, calls)
        }

    @Test
    fun `executeWithRetry throws when max retry attempts exceeded`() =
        runTest {
            var calls = 0
            try {
                executeWithRetry<Unit>(maxAttempts = 3, initialDelay = 1.milliseconds) {
                    calls++
                    throw IOException("Persistent error")
                }
                fail("Expected IOException to be thrown")
            } catch (_: IOException) {
                assertEquals(3, calls)
            }
        }

    @Test
    fun `executeWithRetry throws immediately without retry on non-retryable 404 HTTP error`() =
        runTest {
            var calls = 0
            try {
                executeWithRetry<Unit>(maxAttempts = 3, initialDelay = 1.milliseconds) {
                    calls++
                    throw createHttpException(404)
                }
                fail("Expected HttpException to be thrown")
            } catch (e: HttpException) {
                assertEquals(404, e.code())
                assertEquals(1, calls)
            }
        }

    @Test
    fun `executeWithRetry rethrows CancellationException without retry`() =
        runTest {
            var calls = 0
            try {
                executeWithRetry<Unit>(initialDelay = 1.milliseconds) {
                    calls++
                    throw CancellationException("Cancelled")
                }
                fail("Expected CancellationException")
            } catch (_: CancellationException) {
                assertEquals(1, calls)
            }
        }

    @Test
    fun `isRetryableNetworkError correctly identifies transient errors`() {
        assertTrue(isRetryableNetworkError(IOException("socket closed")))
        assertTrue(isRetryableNetworkError(createHttpException(429)))
        assertTrue(isRetryableNetworkError(createHttpException(500)))
        assertTrue(isRetryableNetworkError(createHttpException(503)))

        assertFalse(isRetryableNetworkError(createHttpException(400)))
        assertFalse(isRetryableNetworkError(createHttpException(401)))
        assertFalse(isRetryableNetworkError(createHttpException(403)))
        assertFalse(isRetryableNetworkError(createHttpException(404)))
        assertFalse(isRetryableNetworkError(IllegalStateException("logic error")))
    }

    private fun createHttpException(code: Int): HttpException {
        val body = "{}".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }
}
