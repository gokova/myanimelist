package com.gokova.myanimelist

import com.gokova.myanimelist.feature.auth.domain.repository.AuthRepository
import com.gokova.myanimelist.feature.auth.domain.usecase.LogoutUseCase
import com.gokova.myanimelist.feature.auth.domain.usecase.ObserveAuthStateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

private class TestAuthRepository : AuthRepository {
    val authFlow = MutableStateFlow(false)
    var logoutCalled = false

    override val isLoggedIn: Flow<Boolean> = authFlow.asStateFlow()

    override suspend fun getAuthorizationUrl(): String = "https://myanimelist.net"

    override suspend fun authenticate(
        code: String,
        state: String?,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun logout() {
        logoutCalled = true
        authFlow.value = false
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial uiState is Loading then emits Authenticated based on auth state`() =
        runTest {
            val repository = TestAuthRepository()
            val observeAuthStateUseCase = ObserveAuthStateUseCase(repository)
            val logoutUseCase = LogoutUseCase(repository)

            val viewModel =
                MainViewModel(
                    observeAuthStateUseCase = observeAuthStateUseCase,
                    logoutUseCase = logoutUseCase,
                )

            val states = mutableListOf<MainUiState>()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect { states.add(it) }
                }

            assertEquals(MainUiState.Loading, states.first())

            advanceUntilIdle()
            assertEquals(MainUiState.Authenticated(isLoggedIn = false), states.last())

            // When user logs in
            repository.authFlow.value = true
            advanceUntilIdle()
            assertEquals(MainUiState.Authenticated(isLoggedIn = true), states.last())

            collectJob.cancel()
        }

    @Test
    fun `logout delegates to logoutUseCase`() =
        runTest {
            val repository = TestAuthRepository()
            val observeAuthStateUseCase = ObserveAuthStateUseCase(repository)
            val logoutUseCase = LogoutUseCase(repository)

            val viewModel =
                MainViewModel(
                    observeAuthStateUseCase = observeAuthStateUseCase,
                    logoutUseCase = logoutUseCase,
                )

            viewModel.logout()
            advanceUntilIdle()

            assertTrue(repository.logoutCalled)
        }
}
