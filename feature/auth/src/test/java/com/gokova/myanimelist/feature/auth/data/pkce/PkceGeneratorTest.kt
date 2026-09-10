package com.gokova.myanimelist.feature.auth.data.pkce

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PkceGeneratorTest {
    private lateinit var generator: PkceGenerator

    @Before
    fun setUp() {
        generator = PkceGenerator()
    }

    @Test
    fun `generateCodeVerifier returns non-empty string of expected max length`() {
        val verifier = generator.generateCodeVerifier()

        assertTrue(verifier.isNotBlank())
        assertTrue(verifier.length <= 128)
        assertTrue(verifier.length >= 43)
    }

    @Test
    fun `generateCodeVerifier returns unique values on subsequent calls`() {
        val verifier1 = generator.generateCodeVerifier()
        val verifier2 = generator.generateCodeVerifier()

        assertNotEquals(verifier1, verifier2)
    }

    @Test
    fun `generateCodeChallenge returns identical verifier for MAL plain PKCE requirement`() {
        val verifier = "test_code_verifier_12345"
        val challenge = generator.generateCodeChallenge(verifier)

        assertEquals(verifier, challenge)
    }

    @Test
    fun `generateState returns non-empty 32-character string`() {
        val state = generator.generateState()

        assertTrue(state.isNotBlank())
        assertEquals(32, state.length)
    }

    @Test
    fun `generateState returns unique values on subsequent calls`() {
        val state1 = generator.generateState()
        val state2 = generator.generateState()

        assertNotEquals(state1, state2)
    }
}
