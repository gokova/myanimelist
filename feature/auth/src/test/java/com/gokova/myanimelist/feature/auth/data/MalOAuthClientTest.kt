package com.gokova.myanimelist.feature.auth.data

import com.gokova.myanimelist.core.network.config.OAuthConfig
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MalOAuthClientTest {
    private val oAuthConfig =
        OAuthConfig(
            clientId = "test_client_id",
            authorizeUrl = "https://myanimelist.net/v1/oauth2/authorize",
            tokenUrl = "https://myanimelist.net/v1/oauth2/token",
        )

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `exchangeCodeForTokens returns Success on 200 OK response`() =
        runTest(testDispatcher) {
            val mockClient =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(
                                """
                                {
                                    "token_type": "bearer",
                                    "expires_in": 2592000,
                                    "access_token": "test_access_token",
                                    "refresh_token": "test_refresh_token"
                                }
                                """.trimIndent().toResponseBody("application/json".toMediaType()),
                            ).build()
                    }.build()

            val client = MalOAuthClient(mockClient, oAuthConfig, testDispatcher)

            val result = client.exchangeCodeForTokens("code_123", "verifier_456")

            assertTrue(result.isSuccess)
            val tokenResponse = result.getOrThrow()
            assertEquals("test_access_token", tokenResponse.accessToken)
            assertEquals("test_refresh_token", tokenResponse.refreshToken)
        }

    @Test
    fun `exchangeCodeForTokens returns Failure on HTTP error response`() =
        runTest(testDispatcher) {
            val mockClient =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(400)
                            .message("Bad Request")
                            .body("Invalid grant".toResponseBody("text/plain".toMediaType()))
                            .build()
                    }.build()

            val client = MalOAuthClient(mockClient, oAuthConfig, testDispatcher)

            val result = client.exchangeCodeForTokens("bad_code", "verifier")

            assertTrue(result.isFailure)
        }

    @Test
    fun `exchangeCodeForTokens returns Failure on IOException`() =
        runTest(testDispatcher) {
            val mockClient =
                OkHttpClient
                    .Builder()
                    .addInterceptor { _ ->
                        throw IOException("Connection reset")
                    }.build()

            val client = MalOAuthClient(mockClient, oAuthConfig, testDispatcher)

            val result = client.exchangeCodeForTokens("code", "verifier")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IOException)
        }
}
