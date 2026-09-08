package com.gokova.myanimelist.feature.auth.domain.usecase

import com.gokova.myanimelist.feature.auth.testing.FakeMalAuthenticator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogoutUseCaseTest {
    private lateinit var authenticator: FakeMalAuthenticator
    private lateinit var useCase: LogoutUseCase

    @Before
    fun setUp() {
        authenticator = FakeMalAuthenticator()
        useCase = LogoutUseCase(authenticator)
    }

    @Test
    fun `invoke calls authenticator logout`() =
        runTest {
            useCase()

            assertTrue(authenticator.logoutCalled)
        }
}
