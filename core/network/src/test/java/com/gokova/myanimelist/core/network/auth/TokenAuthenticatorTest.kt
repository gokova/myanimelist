package com.gokova.myanimelist.core.network.auth

import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.core.network.config.OAuthConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class FakeAuthPreferences : AuthPreferences {
    val accessFlow = MutableStateFlow<String?>(null)
    val refreshFlow = MutableStateFlow<String?>(null)
    val verifierFlow = MutableStateFlow<String?>(null)

    val stateFlow = MutableStateFlow<String?>(null)

    override val accessToken: Flow<String?> = accessFlow
    override val refreshToken: Flow<String?> = refreshFlow
    override val isLoggedIn: Flow<Boolean> = accessFlow.map { it != null }
    override val codeVerifier: Flow<String?> = verifierFlow
    override val oauthState: Flow<String?> = stateFlow

    override suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
    ) {
        accessFlow.value = accessToken
        refreshFlow.value = refreshToken
        verifierFlow.value = null
        stateFlow.value = null
    }

    override suspend fun saveCodeVerifier(verifier: String) {
        verifierFlow.value = verifier
    }

    override suspend fun clearCodeVerifier() {
        verifierFlow.value = null
    }

    override suspend fun saveOAuthState(state: String) {
        stateFlow.value = state
    }

    override suspend fun clearOAuthState() {
        stateFlow.value = null
    }

    override suspend fun clearTokens() {
        accessFlow.value = null
        refreshFlow.value = null
        verifierFlow.value = null
        stateFlow.value = null
    }

    override suspend fun warmCache() {
        // No-op in test fake
    }

    override fun getAccessTokenSync(): String? = accessFlow.value

    override fun getRefreshTokenSync(): String? = refreshFlow.value
}

class TokenAuthenticatorTest {
    private lateinit var preferences: FakeAuthPreferences
    private lateinit var oAuthConfig: OAuthConfig

    @Before
    fun setUp() {
        preferences = FakeAuthPreferences()
        oAuthConfig =
            OAuthConfig(
                clientId = "test_client_id",
            )
    }

    @Test
    fun `authenticate returns null when request is for oauth2 token endpoint`() {
        val authenticator =
            TokenAuthenticator(
                authPreferences = preferences,
                client = OkHttpClient(),
                oAuthConfig = oAuthConfig,
            )

        val request = Request.Builder().url("https://myanimelist.net/v1/oauth2/token").build()
        val response =
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build()

        val nextRequest = authenticator.authenticate(null, response)

        assertNull(nextRequest)
    }

    @Test
    fun `authenticate returns null when retry count exceeds max limit`() {
        val authenticator =
            TokenAuthenticator(
                authPreferences = preferences,
                client = OkHttpClient(),
                oAuthConfig = oAuthConfig,
            )

        val request = Request.Builder().url("https://myanimelist.net/v2/anime").build()
        val r1 =
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build()
        val r2 =
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .priorResponse(r1)
                .build()
        val r3 =
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .priorResponse(r2)
                .build()

        val nextRequest = authenticator.authenticate(null, r3)

        assertNull(nextRequest)
    }

    @Test
    fun `authenticate reuses newly refreshed token if another thread refreshed it already`() {
        val authenticator =
            TokenAuthenticator(
                authPreferences = preferences,
                client = OkHttpClient(),
                oAuthConfig = oAuthConfig,
            )

        preferences.accessFlow.value = "fresh_token_from_other_thread"

        val request =
            Request
                .Builder()
                .url("https://myanimelist.net/v2/anime")
                .header("Authorization", "Bearer stale_token")
                .build()

        val response =
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build()

        val nextRequest = authenticator.authenticate(null, response)

        assertEquals("Bearer fresh_token_from_other_thread", nextRequest?.header("Authorization"))
    }

    @Test
    fun `authenticate refreshes token and attaches to request on successful response`() {
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
                                "access_token": "new_access_token_123",
                                "refresh_token": "new_refresh_token_456"
                            }
                            """.trimIndent().toResponseBody("application/json".toMediaType()),
                        ).build()
                }.build()

        val authenticator =
            TokenAuthenticator(
                authPreferences = preferences,
                client = mockClient,
                oAuthConfig = oAuthConfig,
            )

        preferences.accessFlow.value = "stale_token"
        preferences.refreshFlow.value = "valid_refresh_token"

        val request =
            Request
                .Builder()
                .url("https://myanimelist.net/v2/anime")
                .header("Authorization", "Bearer stale_token")
                .build()

        val response =
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build()

        val nextRequest = authenticator.authenticate(null, response)

        assertEquals("Bearer new_access_token_123", nextRequest?.header("Authorization"))
        assertEquals("new_access_token_123", preferences.accessFlow.value)
        assertEquals("new_refresh_token_456", preferences.refreshFlow.value)
    }

    @Test
    fun `authenticate clears tokens when refresh call fails`() {
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
                        .body("{}".toResponseBody("application/json".toMediaType()))
                        .build()
                }.build()

        val authenticator =
            TokenAuthenticator(
                authPreferences = preferences,
                client = mockClient,
                oAuthConfig = oAuthConfig,
            )

        preferences.accessFlow.value = "stale_token"
        preferences.refreshFlow.value = "invalid_refresh_token"

        val request =
            Request
                .Builder()
                .url("https://myanimelist.net/v2/anime")
                .header("Authorization", "Bearer stale_token")
                .build()

        val response =
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build()

        val nextRequest = authenticator.authenticate(null, response)

        assertNull(nextRequest)
        assertNull(preferences.accessFlow.value)
        assertNull(preferences.refreshFlow.value)
    }
}
