package com.gokova.myanimelist.navigation

import com.gokova.myanimelist.feature.auth.OAuthRedirectResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OAuthRedirectHandlerTest {
    private lateinit var manager: OAuthRedirectHandler

    @Before
    fun setUp() {
        manager = OAuthRedirectHandler()
    }

    @Test
    fun `handleUri with valid authorization code and state parses Success`() {
        val params =
            mapOf(
                "code" to "auth_code_12345",
                "state" to "csrf_state_xyz",
            )

        val handled =
            manager.handleUri(
                scheme = "com.gokova.myanimelist",
                host = "oauth2redirect",
                getQueryParameter = { params[it] },
            )

        assertTrue(handled)
        val expected = OAuthRedirectResult.Success(code = "auth_code_12345", state = "csrf_state_xyz")
        assertEquals(expected, manager.redirectResult.value)
    }

    @Test
    fun `consumeResult resets redirectResult to null`() {
        val params = mapOf("code" to "sample_code")
        manager.handleUri(
            scheme = "com.gokova.myanimelist",
            host = "oauth2redirect",
            getQueryParameter = { params[it] },
        )

        assertTrue(manager.redirectResult.value != null)

        manager.consumeResult()

        assertNull(manager.redirectResult.value)
    }

    @Test
    fun `handleUri with error parses Error result with description`() {
        val params =
            mapOf(
                "error" to "access_denied",
                "error_description" to "The user denied the request",
            )

        val handled =
            manager.handleUri(
                scheme = "com.gokova.myanimelist",
                host = "oauth2redirect",
                getQueryParameter = { params[it] },
            )

        assertTrue(handled)
        val expected =
            OAuthRedirectResult.Error(
                error = "access_denied",
                description = "The user denied the request",
            )
        assertEquals(expected, manager.redirectResult.value)
    }

    @Test
    fun `handleUri with mismatched scheme or host returns false and ignores callback`() {
        val params = mapOf("code" to "auth_code_12345")

        val wrongSchemeHandled =
            manager.handleUri(
                scheme = "https",
                host = "oauth2redirect",
                getQueryParameter = { params[it] },
            )
        assertFalse(wrongSchemeHandled)
        assertNull(manager.redirectResult.value)

        val wrongHostHandled =
            manager.handleUri(
                scheme = "com.gokova.myanimelist",
                host = "other_host",
                getQueryParameter = { params[it] },
            )
        assertFalse(wrongHostHandled)
        assertNull(manager.redirectResult.value)
    }

    @Test
    fun `logout and subsequent new login flow prevents stale code reuse`() {
        // Step 1: Initial login receives redirect code
        val firstParams = mapOf("code" to "code_session_1")
        manager.handleUri(
            scheme = "com.gokova.myanimelist",
            host = "oauth2redirect",
            getQueryParameter = { firstParams[it] },
        )
        assertEquals(
            OAuthRedirectResult.Success("code_session_1", null),
            manager.redirectResult.value,
        )

        // Step 2: AuthScreen consumes the code immediately
        manager.consumeResult()
        assertNull(manager.redirectResult.value)

        // Step 3: User logs out, navigating back to AuthScreen
        // Assert that redirectResult is completely null and no stale code exists
        assertNull(manager.redirectResult.value)

        // Step 4: Subsequent login receives a brand-new code
        val secondParams = mapOf("code" to "code_session_2")
        manager.handleUri(
            scheme = "com.gokova.myanimelist",
            host = "oauth2redirect",
            getQueryParameter = { secondParams[it] },
        )
        assertEquals(
            OAuthRedirectResult.Success("code_session_2", null),
            manager.redirectResult.value,
        )
    }
}
