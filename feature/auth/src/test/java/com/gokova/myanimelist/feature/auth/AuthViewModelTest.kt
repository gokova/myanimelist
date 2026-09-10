package com.gokova.myanimelist.feature.auth

import com.gokova.myanimelist.feature.auth.domain.usecase.GetAuthUrlUseCase
import com.gokova.myanimelist.feature.auth.domain.usecase.LoginWithCodeUseCase
import com.gokova.myanimelist.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        val repository = FakeAuthRepository()
        val viewModel =
            AuthViewModel(
                getAuthUrlUseCase = GetAuthUrlUseCase(repository),
                loginWithCodeUseCase = LoginWithCodeUseCase(repository),
            )

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `onLoginClicked emits OpenOAuthUrl event and stays Idle`() =
        runTest {
            val repository = FakeAuthRepository()
            val expectedUrl = "https://myanimelist.net/v1/oauth2/authorize?code_challenge=test"
            repository.authUrlToReturn = expectedUrl

            val viewModel =
                AuthViewModel(
                    getAuthUrlUseCase = GetAuthUrlUseCase(repository),
                    loginWithCodeUseCase = LoginWithCodeUseCase(repository),
                )

            var receivedEvent: AuthUiEvent? = null
            val job =
                launch {
                    receivedEvent = viewModel.events.first()
                }

            viewModel.onLoginClicked()
            advanceUntilIdle()

            assertTrue(receivedEvent is AuthUiEvent.OpenOAuthUrl)
            assertEquals(expectedUrl, (receivedEvent as AuthUiEvent.OpenOAuthUrl).url)
            assertEquals(AuthUiState.Idle, viewModel.uiState.value)

            job.cancel()
        }

    @Test
    fun `handleAuthorizationCode transitions to Success and emits AuthSuccess on valid code`() =
        runTest {
            val repository = FakeAuthRepository()
            repository.authenticateResult = Result.success(Unit)
            val viewModel =
                AuthViewModel(
                    getAuthUrlUseCase = GetAuthUrlUseCase(repository),
                    loginWithCodeUseCase = LoginWithCodeUseCase(repository),
                )

            var receivedEvent: AuthUiEvent? = null
            val job =
                launch {
                    receivedEvent = viewModel.events.first()
                }

            viewModel.handleAuthorizationCode("sample_valid_code", "csrf_state_123")
            advanceUntilIdle()

            assertEquals(AuthUiState.Idle, viewModel.uiState.value)
            assertEquals(AuthUiEvent.AuthSuccess, receivedEvent)
            assertEquals("sample_valid_code", repository.authenticateCalledWith)
            assertEquals("csrf_state_123", repository.authenticateCalledWithState)

            job.cancel()
        }

    @Test
    fun `handleAuthorizationCode transitions to Error on failure`() =
        runTest {
            val repository = FakeAuthRepository()
            repository.authenticateResult = Result.failure(RuntimeException("Invalid code"))
            val viewModel =
                AuthViewModel(
                    getAuthUrlUseCase = GetAuthUrlUseCase(repository),
                    loginWithCodeUseCase = LoginWithCodeUseCase(repository),
                )

            viewModel.handleAuthorizationCode("sample_bad_code")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is AuthUiState.Error)
            assertEquals(R.string.feature_auth_error_login_failed, (state as AuthUiState.Error).messageResId)
        }

    @Test
    fun `handleRedirectError with access_denied gracefully stays Idle`() {
        val repository = FakeAuthRepository()
        val viewModel =
            AuthViewModel(
                getAuthUrlUseCase = GetAuthUrlUseCase(repository),
                loginWithCodeUseCase = LoginWithCodeUseCase(repository),
            )

        viewModel.handleRedirectError(error = "access_denied")

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `handleRedirectError with general error transitions to Error`() {
        val repository = FakeAuthRepository()
        val viewModel =
            AuthViewModel(
                getAuthUrlUseCase = GetAuthUrlUseCase(repository),
                loginWithCodeUseCase = LoginWithCodeUseCase(repository),
            )

        viewModel.handleRedirectError(error = "server_error")

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals(R.string.feature_auth_error_login_failed, (state as AuthUiState.Error).messageResId)
    }
}
