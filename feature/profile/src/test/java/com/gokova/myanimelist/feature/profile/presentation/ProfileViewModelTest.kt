package com.gokova.myanimelist.feature.profile.presentation

import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.domain.repository.ProfileRepository
import com.gokova.myanimelist.core.domain.usecase.ObserveUserProfileUseCase
import com.gokova.myanimelist.core.domain.usecase.RefreshUserProfileUseCase
import com.gokova.myanimelist.feature.profile.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeProfileRepository
    private lateinit var observeUseCase: ObserveUserProfileUseCase
    private lateinit var refreshUseCase: RefreshUserProfileUseCase
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeProfileRepository()
        observeUseCase = ObserveUserProfileUseCase(fakeRepository)
        refreshUseCase = RefreshUserProfileUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state emits content when profile is cached`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "cached_user")
            fakeRepository.profileFlow.value = cached
            fakeRepository.refreshResult = Result.success(cached)

            viewModel = ProfileViewModel(observeUseCase, refreshUseCase)
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Content)
            assertEquals(cached, (state as ProfileUiState.Content).userProfile)
            assertFalse(state.showLogoutDialog)
            collectJob.cancel()
        }

    @Test
    fun `refresh failure emits error when cache is empty`() =
        runTest(testDispatcher) {
            fakeRepository.refreshResult = Result.failure(IOException("Failed to connect"))

            viewModel = ProfileViewModel(observeUseCase, refreshUseCase)
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Error)
            assertEquals(R.string.profile_load_failed, (state as ProfileUiState.Error).messageResId)
            collectJob.cancel()
        }

    @Test
    fun `cached content exposes refreshing state while refresh is in progress`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "cached_user")
            fakeRepository.profileFlow.value = cached
            fakeRepository.refreshGate = CompletableDeferred()

            viewModel = ProfileViewModel(observeUseCase, refreshUseCase)
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val refreshingState = viewModel.uiState.value as ProfileUiState.Content
            assertTrue(refreshingState.isRefreshing)

            fakeRepository.refreshGate?.complete(Unit)
            advanceUntilIdle()

            val loadedState = viewModel.uiState.value as ProfileUiState.Content
            assertFalse(loadedState.isRefreshing)
            collectJob.cancel()
        }

    @Test
    fun `onLogoutClicked shows dialog and onLogoutDismissed hides dialog`() =
        runTest(testDispatcher) {
            fakeRepository.profileFlow.value = UserProfile(id = 1L, name = "user")
            viewModel = ProfileViewModel(observeUseCase, refreshUseCase)
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.onLogoutClicked()
            advanceUntilIdle()
            var state = viewModel.uiState.value as ProfileUiState.Content
            assertTrue(state.showLogoutDialog)

            viewModel.onLogoutDismissed()
            advanceUntilIdle()
            state = viewModel.uiState.value as ProfileUiState.Content
            assertFalse(state.showLogoutDialog)
            collectJob.cancel()
        }

    @Test
    fun `onLogoutConfirmed sends RequestLogout event and closes dialog`() =
        runTest(testDispatcher) {
            fakeRepository.profileFlow.value = UserProfile(id = 1L, name = "user")
            viewModel = ProfileViewModel(observeUseCase, refreshUseCase)
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.onLogoutClicked()
            advanceUntilIdle()

            var emittedEvent: ProfileUiEvent? = null
            val eventJob = launch { emittedEvent = viewModel.uiEvent.first() }

            viewModel.onLogoutConfirmed()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertFalse(state.showLogoutDialog)
            assertEquals(ProfileUiEvent.RequestLogout, emittedEvent)
            eventJob.cancel()
            collectJob.cancel()
        }

    @Test
    fun `onBackClicked sends NavigateBack event`() =
        runTest(testDispatcher) {
            viewModel = ProfileViewModel(observeUseCase, refreshUseCase)
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            var emittedEvent: ProfileUiEvent? = null
            val eventJob = launch { emittedEvent = viewModel.uiEvent.first() }

            viewModel.onBackClicked()
            advanceUntilIdle()

            assertEquals(ProfileUiEvent.NavigateBack, emittedEvent)
            eventJob.cancel()
            collectJob.cancel()
        }

    private class FakeProfileRepository : ProfileRepository {
        val profileFlow = MutableStateFlow<UserProfile?>(null)
        var refreshGate: CompletableDeferred<Unit>? = null
        var refreshResult: Result<UserProfile> =
            Result.success(
                UserProfile(id = 1L, name = "refreshed"),
            )

        override val userProfile: Flow<UserProfile?> = profileFlow

        override suspend fun refreshUserProfile(): Result<UserProfile> {
            refreshGate?.await()
            return refreshResult.onSuccess { profileFlow.value = it }
        }

        override suspend fun clearUserProfile() {
            profileFlow.value = null
        }
    }
}
