package com.gokova.myanimelist

import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.domain.repository.ProfileRepository
import com.gokova.myanimelist.core.domain.usecase.ObserveUserProfileUseCase
import com.gokova.myanimelist.core.domain.usecase.RefreshUserProfileUseCase
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

private class TestProfileRepository : ProfileRepository {
    val profileFlow = MutableStateFlow<UserProfile?>(null)
    var refreshCalled = false

    override val userProfile: Flow<UserProfile?> = profileFlow.asStateFlow()

    override suspend fun refreshUserProfile(): Result<UserProfile> {
        refreshCalled = true
        val profile =
            UserProfile(
                id = 1L,
                name = "TestUser",
                pictureUrl = "https://example.com/avatar.jpg",
                gender = "male",
                birthday = "2000-01-01",
                location = "Tokyo",
                joinedAt = "2020-01-01T00:00:00Z",
                statistics = null,
            )
        profileFlow.value = profile
        return Result.success(profile)
    }

    override suspend fun clearUserProfile() {
        profileFlow.value = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial uiState is Loading then emits Authenticated based on auth state and profile`() =
        runTest {
            val authRepository = TestAuthRepository()
            val profileRepository = TestProfileRepository()
            val observeAuthStateUseCase = ObserveAuthStateUseCase(authRepository)
            val logoutUseCase = LogoutUseCase(authRepository)
            val observeUserProfileUseCase = ObserveUserProfileUseCase(profileRepository)
            val refreshUserProfileUseCase = RefreshUserProfileUseCase(profileRepository)

            val viewModel =
                MainViewModel(
                    observeAuthStateUseCase = observeAuthStateUseCase,
                    logoutUseCase = logoutUseCase,
                    observeUserProfileUseCase = observeUserProfileUseCase,
                    refreshUserProfileUseCase = refreshUserProfileUseCase,
                )

            val states = mutableListOf<MainUiState>()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect { states.add(it) }
                }

            assertEquals(MainUiState.Loading, states.first())

            advanceUntilIdle()
            assertEquals(
                MainUiState.Authenticated(isLoggedIn = false, avatarUrl = null),
                states.last(),
            )

            // When user logs in
            authRepository.authFlow.value = true
            advanceUntilIdle()

            assertTrue(profileRepository.refreshCalled)
            assertEquals(
                MainUiState.Authenticated(
                    isLoggedIn = true,
                    avatarUrl = "https://example.com/avatar.jpg",
                ),
                states.last(),
            )

            collectJob.cancel()
        }

    @Test
    fun `logout delegates to logoutUseCase`() =
        runTest {
            val authRepository = TestAuthRepository()
            val profileRepository = TestProfileRepository()
            val observeAuthStateUseCase = ObserveAuthStateUseCase(authRepository)
            val logoutUseCase = LogoutUseCase(authRepository)
            val observeUserProfileUseCase = ObserveUserProfileUseCase(profileRepository)
            val refreshUserProfileUseCase = RefreshUserProfileUseCase(profileRepository)

            val viewModel =
                MainViewModel(
                    observeAuthStateUseCase = observeAuthStateUseCase,
                    logoutUseCase = logoutUseCase,
                    observeUserProfileUseCase = observeUserProfileUseCase,
                    refreshUserProfileUseCase = refreshUserProfileUseCase,
                )

            viewModel.logout()
            advanceUntilIdle()

            assertTrue(authRepository.logoutCalled)
        }

    @Test
    fun `refreshProfile delegates to refreshUserProfileUseCase`() =
        runTest {
            val authRepository = TestAuthRepository()
            val profileRepository = TestProfileRepository()
            val observeAuthStateUseCase = ObserveAuthStateUseCase(authRepository)
            val logoutUseCase = LogoutUseCase(authRepository)
            val observeUserProfileUseCase = ObserveUserProfileUseCase(profileRepository)
            val refreshUserProfileUseCase = RefreshUserProfileUseCase(profileRepository)

            val viewModel =
                MainViewModel(
                    observeAuthStateUseCase = observeAuthStateUseCase,
                    logoutUseCase = logoutUseCase,
                    observeUserProfileUseCase = observeUserProfileUseCase,
                    refreshUserProfileUseCase = refreshUserProfileUseCase,
                )

            viewModel.refreshProfile()
            advanceUntilIdle()

            assertTrue(profileRepository.refreshCalled)
        }
}
