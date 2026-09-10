package com.gokova.myanimelist.feature.auth.data.repository

import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.core.network.config.OAuthConfig
import com.gokova.myanimelist.feature.auth.data.MalOAuthClient
import com.gokova.myanimelist.feature.auth.data.pkce.PkceGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        // No-op for in-memory fake
    }

    override fun getAccessTokenSync(): String? = accessFlow.value

    override fun getRefreshTokenSync(): String? = refreshFlow.value
}

class AuthRepositoryImplTest {
    private lateinit var preferences: FakeAuthPreferences
    private lateinit var pkceGenerator: PkceGenerator
    private lateinit var oAuthConfig: OAuthConfig

    @Before
    fun setUp() {
        preferences = FakeAuthPreferences()
        pkceGenerator = PkceGenerator()
        oAuthConfig =
            OAuthConfig(
                clientId = "test_client_id",
                authorizeUrl = "https://myanimelist.net/v1/oauth2/authorize",
                tokenUrl = "https://myanimelist.net/v1/oauth2/token",
            )
    }

    @Test
    fun `getAuthorizationUrl builds URL using OAuthConfig and saves verifier and state`() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    authPreferences = preferences,
                    pkceGenerator = pkceGenerator,
                    oAuthClient = MalOAuthClient(OkHttpClient(), oAuthConfig),
                    oAuthConfig = oAuthConfig,
                )

            val url = repository.getAuthorizationUrl()

            assertTrue(url.startsWith(oAuthConfig.authorizeUrl))
            assertTrue(url.contains("client_id=${oAuthConfig.clientId}"))
            assertTrue(url.contains("code_challenge_method=plain"))

            // Verify PKCE verifier was saved
            val savedVerifier = preferences.codeVerifier.first()
            assertTrue(!savedVerifier.isNullOrBlank())
            assertTrue(url.contains("code_challenge=$savedVerifier"))

            // Verify CSRF state was saved
            val savedState = preferences.oauthState.first()
            assertTrue(!savedState.isNullOrBlank())
            assertTrue(url.contains("state=$savedState"))
        }

    @Test
    fun `authenticate returns failure when state does not match`() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    authPreferences = preferences,
                    pkceGenerator = pkceGenerator,
                    oAuthClient = MalOAuthClient(OkHttpClient(), oAuthConfig),
                    oAuthConfig = oAuthConfig,
                )

            preferences.saveOAuthState("expected_state")
            preferences.saveCodeVerifier("mock_verifier")

            val result = repository.authenticate(code = "any_code", state = "mismatched_state")

            assertTrue(result.isFailure)
            assertNull(preferences.oauthState.first())
            assertNull(preferences.codeVerifier.first())
        }

    @Test
    fun `authenticate returns failure when verifier is missing`() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    authPreferences = preferences,
                    pkceGenerator = pkceGenerator,
                    oAuthClient = MalOAuthClient(OkHttpClient(), oAuthConfig),
                    oAuthConfig = oAuthConfig,
                )

            val result = repository.authenticate("any_code")

            assertTrue(result.isFailure)
        }

    @Test
    fun `authenticate successfully exchanges code and persists tokens`() =
        runTest {
            val okHttpClient =
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
                                    "access_token": "mock_access_token",
                                    "refresh_token": "mock_refresh_token"
                                }
                                """.trimIndent().toResponseBody("application/json".toMediaType()),
                            ).build()
                    }.build()

            val oAuthClient = MalOAuthClient(okHttpClient, oAuthConfig)
            val repository =
                AuthRepositoryImpl(
                    authPreferences = preferences,
                    pkceGenerator = pkceGenerator,
                    oAuthClient = oAuthClient,
                    oAuthConfig = oAuthConfig,
                )

            preferences.saveCodeVerifier("mock_verifier")
            preferences.saveOAuthState("mock_state")

            val result = repository.authenticate(code = "valid_code", state = "mock_state")

            assertTrue(result.isSuccess)
            assertEquals("mock_access_token", preferences.accessToken.first())
            assertEquals("mock_refresh_token", preferences.refreshToken.first())
            assertNull(preferences.codeVerifier.first())
            assertNull(preferences.oauthState.first())
        }

    @Test
    fun `logout clears stored tokens and verifier`() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    authPreferences = preferences,
                    pkceGenerator = pkceGenerator,
                    oAuthClient = MalOAuthClient(OkHttpClient(), oAuthConfig),
                    oAuthConfig = oAuthConfig,
                )

            preferences.saveTokens("token_a", "token_b")
            preferences.saveCodeVerifier("verifier_a")
            preferences.saveOAuthState("state_a")

            repository.logout()

            assertNull(preferences.accessToken.first())
            assertNull(preferences.refreshToken.first())
            assertNull(preferences.codeVerifier.first())
            assertNull(preferences.oauthState.first())
        }
}
