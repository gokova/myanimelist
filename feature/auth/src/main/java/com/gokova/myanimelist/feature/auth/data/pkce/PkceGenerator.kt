package com.gokova.myanimelist.feature.auth.data.pkce

import java.security.SecureRandom
import javax.inject.Inject

/**
 * Generates PKCE (Proof Key for Code Exchange) parameters for MyAnimeList OAuth 2.0.
 *
 * NOTE ON MAL PKCE REQUIREMENT:
 * The MyAnimeList OAuth 2.0 API specifically requires `code_challenge_method = "plain"`.
 * Under the plain method, `code_challenge` MUST be identical to the `code_verifier`.
 * Using S256 (SHA-256) will cause MAL authorization to fail.
 *
 * RFC 7636 Section 4.1 specifies:
 * code-verifier = 43*128unreserved
 * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
 */
class PkceGenerator
    @Inject
    constructor() {
        fun generateCodeVerifier(): String {
            val secureRandom = SecureRandom()
            val charArray = CharArray(CODE_VERIFIER_LENGTH)
            for (i in 0 until CODE_VERIFIER_LENGTH) {
                charArray[i] = ALLOWED_CHARS[secureRandom.nextInt(ALLOWED_CHARS.length)]
            }
            return String(charArray)
        }

        fun generateCodeChallenge(verifier: String): String = verifier

        fun generateState(length: Int = STATE_LENGTH): String {
            val secureRandom = SecureRandom()
            val charArray = CharArray(length)
            for (i in 0 until length) {
                charArray[i] = ALLOWED_CHARS[secureRandom.nextInt(ALLOWED_CHARS.length)]
            }
            return String(charArray)
        }

        companion object {
            private const val CODE_VERIFIER_LENGTH = 128
            private const val STATE_LENGTH = 32
            private const val ALLOWED_CHARS =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._"
        }
    }
