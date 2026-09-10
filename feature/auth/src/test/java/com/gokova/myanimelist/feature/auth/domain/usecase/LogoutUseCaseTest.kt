package com.gokova.myanimelist.feature.auth.domain.usecase

import com.gokova.myanimelist.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogoutUseCaseTest {
    private lateinit var repository: FakeAuthRepository
    private lateinit var useCase: LogoutUseCase

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        useCase = LogoutUseCase(repository)
    }

    @Test
    fun `invoke calls repository logout`() =
        runTest {
            useCase()

            assertTrue(repository.logoutCalled)
        }
}
