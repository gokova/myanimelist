package com.gokova.myanimelist.feature.mylist.presentation

import com.gokova.myanimelist.feature.mylist.R
import com.gokova.myanimelist.feature.mylist.domain.model.AiringStatus
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnimeStatus
import com.gokova.myanimelist.feature.mylist.domain.usecase.ObserveUserAnimeListUseCase
import com.gokova.myanimelist.feature.mylist.domain.usecase.SyncUserAnimeListUseCase
import com.gokova.myanimelist.feature.mylist.testing.FakeMyListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class MyListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createAnime(
        id: Long,
        title: String,
        status: UserAnimeStatus,
        score: Int,
    ) = UserAnime(
        id = id,
        originalTitle = title,
        englishTitle = title,
        displayTitle = title,
        subtitleTitle = null,
        imageUrl = null,
        mediaType = "TV",
        airingStatus = AiringStatus.FINISHED_AIRING,
        releaseSeason = "2020 Fall",
        totalEpisodes = 12,
        userStatus = status,
        userScore = score,
        watchedEpisodes = 12,
        isRewatching = false,
        updatedAt = "2023-01-01",
    )

    @Test
    fun `init triggers sync and emits syncing snackbar`() =
        runTest {
            val repository = FakeMyListRepository()
            val observeUseCase = ObserveUserAnimeListUseCase(repository)
            val syncUseCase = SyncUserAnimeListUseCase(repository)

            val viewModel = MyListViewModel(observeUseCase, syncUseCase)

            val events = mutableListOf<MyListUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            advanceUntilIdle()

            assertEquals(1, repository.syncCallCount)
            assertEquals(false, repository.lastForceFlag)
            assertEquals(1, events.size)
            assertTrue(events[0] is MyListUiEvent.ShowSnackbar)
            val snackbarEvent = events[0] as MyListUiEvent.ShowSnackbar
            assertEquals(R.string.my_list_syncing_message, snackbarEvent.messageRes)
            assertFalse(snackbarEvent.isError)

            job.cancel()
        }

    @Test
    fun `category selection updates uiState and filters anime`() =
        runTest {
            val repository = FakeMyListRepository()
            val animeWatching = createAnime(1L, "Watching Anime", UserAnimeStatus.WATCHING, 9)
            val animeCompleted = createAnime(2L, "Completed Anime", UserAnimeStatus.COMPLETED, 10)
            repository.setAnimeList(listOf(animeWatching, animeCompleted))

            val observeUseCase = ObserveUserAnimeListUseCase(repository)
            val syncUseCase = SyncUserAnimeListUseCase(repository)
            val viewModel = MyListViewModel(observeUseCase, syncUseCase)

            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.animeList.size)

            viewModel.onCategorySelected(ListFilterCategory.WATCHING)
            advanceUntilIdle()

            assertEquals(ListFilterCategory.WATCHING, viewModel.uiState.value.selectedCategory)
            assertEquals(1, viewModel.uiState.value.animeList.size)
            assertEquals(
                "Watching Anime",
                viewModel.uiState.value.animeList[0]
                    .displayTitle,
            )

            collectJob.cancel()
        }

    @Test
    fun `onRefresh forces sync and handles error with retry snackbar`() =
        runTest {
            val repository = FakeMyListRepository()
            val observeUseCase = ObserveUserAnimeListUseCase(repository)
            val syncUseCase = SyncUserAnimeListUseCase(repository)
            val viewModel = MyListViewModel(observeUseCase, syncUseCase)

            advanceUntilIdle()

            repository.shouldFailSync = true
            val events = mutableListOf<MyListUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            viewModel.onRefresh()
            advanceUntilIdle()

            assertEquals(2, repository.syncCallCount)
            assertEquals(true, repository.lastForceFlag)

            val errorEvent =
                events.firstOrNull {
                    (it as? MyListUiEvent.ShowSnackbar)?.isError == true
                }
            assertTrue(errorEvent is MyListUiEvent.ShowSnackbar)
            val snackbar = errorEvent as MyListUiEvent.ShowSnackbar
            assertEquals(R.string.my_list_sync_failed_message, snackbar.messageRes)
            assertEquals(R.string.my_list_retry, snackbar.actionRes)

            val syncingEvent =
                events.firstOrNull {
                    (it as? MyListUiEvent.ShowSnackbar)?.messageRes ==
                        R.string.my_list_syncing_message
                }
            assertTrue(syncingEvent is MyListUiEvent.ShowSnackbar)

            job.cancel()
        }

    @Test
    fun `init skips syncing snackbar when inside cooldown`() =
        runTest {
            val repository = FakeMyListRepository()
            repository.shouldSkipDueToCooldown = true
            val observeUseCase = ObserveUserAnimeListUseCase(repository)
            val syncUseCase = SyncUserAnimeListUseCase(repository)

            val viewModel = MyListViewModel(observeUseCase, syncUseCase)

            val events = mutableListOf<MyListUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            advanceUntilIdle()

            assertEquals(1, repository.syncCallCount)
            assertEquals(0, events.size)
            assertFalse(viewModel.uiState.value.isSyncing)

            job.cancel()
        }
}
