package com.gokova.myanimelist.feature.auth.domain.usecase

import com.gokova.myanimelist.feature.auth.testing.FakeMalAuthenticator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginWithCodeUseCaseTest {
    private lateinit var authenticator: FakeMalAuthenticator
    private lateinit var useCase: LoginWithCodeUseCase

    @Before
    fun setUp() {
        authenticator = FakeMalAuthenticator()
        useCase = LoginWithCodeUseCase(authenticator)
    }

    @Test
    fun `invoke delegates code to authenticator and returns success`() =
        runTest {
            val code = "test_auth_code_123"
            authenticator.authenticateResult = Result.success(Unit)

            val result = useCase(code)

            assertEquals(code, authenticator.authenticateCalledWith)
            assertTrue(result.isSuccess)
        }

    @Test
    fun `invoke returns failure when authenticator fails`() =
        runTest {
            val exception = RuntimeException("OAuth server error")
            authenticator.authenticateResult = Result.failure(exception)

            val result = useCase("invalid_code")

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
}
