package com.gokova.myanimelist.feature.auth

import com.gokova.myanimelist.feature.auth.domain.usecase.GetAuthUrlUseCase
import com.gokova.myanimelist.feature.auth.domain.usecase.LoginWithCodeUseCase
import com.gokova.myanimelist.feature.auth.testing.FakeMalAuthenticator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial uiState is Idle`() {
        val authenticator = FakeMalAuthenticator()
        val viewModel =
            AuthViewModel(
                getAuthUrlUseCase = GetAuthUrlUseCase(authenticator),
                loginWithCodeUseCase = LoginWithCodeUseCase(authenticator),
            )

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `handleAuthorizationCode transitions to Success on valid code`() =
        runTest {
            val authenticator = FakeMalAuthenticator()
            authenticator.authenticateResult = Result.success(Unit)
            val viewModel =
                AuthViewModel(
                    getAuthUrlUseCase = GetAuthUrlUseCase(authenticator),
                    loginWithCodeUseCase = LoginWithCodeUseCase(authenticator),
                )

            viewModel.handleAuthorizationCode("sample_valid_code")
            advanceUntilIdle()

            assertEquals(AuthUiState.Success, viewModel.uiState.value)
        }

    @Test
    fun `handleAuthorizationCode transitions to Error on failure`() =
        runTest {
            val authenticator = FakeMalAuthenticator()
            authenticator.authenticateResult = Result.failure(RuntimeException("Invalid code"))
            val viewModel =
                AuthViewModel(
                    getAuthUrlUseCase = GetAuthUrlUseCase(authenticator),
                    loginWithCodeUseCase = LoginWithCodeUseCase(authenticator),
                )

            viewModel.handleAuthorizationCode("sample_bad_code")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is AuthUiState.Error)
        }
}
