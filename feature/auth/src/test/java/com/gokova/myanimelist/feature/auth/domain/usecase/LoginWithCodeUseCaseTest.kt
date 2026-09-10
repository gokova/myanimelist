package com.gokova.myanimelist.feature.auth.domain.usecase

import com.gokova.myanimelist.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginWithCodeUseCaseTest {
    private lateinit var repository: FakeAuthRepository
    private lateinit var useCase: LoginWithCodeUseCase

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        useCase = LoginWithCodeUseCase(repository)
    }

    @Test
    fun `invoke delegates code to repository and returns success`() =
        runTest {
            val code = "test_auth_code_123"
            repository.authenticateResult = Result.success(Unit)

            val result = useCase(code)

            assertEquals(code, repository.authenticateCalledWith)
            assertTrue(result.isSuccess)
        }

    @Test
    fun `invoke delegates code and state to repository`() =
        runTest {
            val code = "test_auth_code_123"
            val state = "csrf_state_456"
            repository.authenticateResult = Result.success(Unit)

            val result = useCase(code, state)

            assertEquals(code, repository.authenticateCalledWith)
            assertEquals(state, repository.authenticateCalledWithState)
            assertTrue(result.isSuccess)
        }

    @Test
    fun `invoke returns failure when repository fails`() =
        runTest {
            val exception = RuntimeException("OAuth server error")
            repository.authenticateResult = Result.failure(exception)

            val result = useCase("invalid_code")

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
}
