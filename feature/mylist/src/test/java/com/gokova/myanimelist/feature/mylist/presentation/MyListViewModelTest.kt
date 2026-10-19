package com.gokova.myanimelist.feature.mylist.presentation

import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.model.UserAnimeListUpdate
import com.gokova.myanimelist.core.domain.repository.UserAnimeListStatusRepository
import com.gokova.myanimelist.core.domain.usecase.DeleteAnimeFromListUseCase
import com.gokova.myanimelist.core.domain.usecase.UpdateAnimeStatusUseCase
import com.gokova.myanimelist.feature.mylist.R
import com.gokova.myanimelist.feature.mylist.domain.model.AiringStatus
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnimeStatus
import com.gokova.myanimelist.feature.mylist.domain.usecase.ObserveUserAnimeListUseCase
import com.gokova.myanimelist.feature.mylist.domain.usecase.SyncUserAnimeListUseCase
import com.gokova.myanimelist.feature.mylist.testing.FakeMyListRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.IOException
import com.gokova.myanimelist.core.ui.R as CoreUiR

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
        watchedEpisodes: Int = 5,
    ) = UserAnime(
        id = id,
        originalTitle = title,
        englishTitle = title,
        displayTitle = title,
        subtitleTitle = null,
        thumbnailUrl = null,
        largeImageUrl = null,
        mediaType = "TV",
        airingStatus = AiringStatus.FINISHED_AIRING,
        releaseSeason = "2020 Fall",
        totalEpisodes = 12,
        userStatus = status,
        userScore = score,
        watchedEpisodes = watchedEpisodes,
        isRewatching = false,
        updatedAt = "2023-01-01",
    )

    private fun createViewModel(
        repository: FakeMyListRepository = FakeMyListRepository(),
        statusRepository: FakeUserAnimeListStatusRepository = FakeUserAnimeListStatusRepository(),
    ): MyListViewModel {
        val observeUseCase = ObserveUserAnimeListUseCase(repository)
        val syncUseCase = SyncUserAnimeListUseCase(repository)
        val updateUseCase = UpdateAnimeStatusUseCase(statusRepository)
        val deleteUseCase = DeleteAnimeFromListUseCase(statusRepository)
        return MyListViewModel(
            observeUserAnimeListUseCase = observeUseCase,
            syncUserAnimeListUseCase = syncUseCase,
            updateAnimeStatusUseCase = updateUseCase,
            deleteAnimeFromListUseCase = deleteUseCase,
        )
    }

    @Test
    fun `init triggers sync and emits syncing snackbar`() =
        runTest {
            val repository = FakeMyListRepository()
            val viewModel = createViewModel(repository = repository)

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

            val viewModel = createViewModel(repository = repository)

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
    fun `onSync forces sync and handles error with retry snackbar`() =
        runTest {
            val repository = FakeMyListRepository()
            val viewModel = createViewModel(repository = repository)

            advanceUntilIdle()

            repository.shouldFailSync = true
            val events = mutableListOf<MyListUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            viewModel.onSync(isPullToRefresh = true, force = true)
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

            job.cancel()
        }

    @Test
    fun `init skips syncing snackbar when inside cooldown`() =
        runTest {
            val repository = FakeMyListRepository()
            repository.shouldSkipDueToCooldown = true
            val viewModel = createViewModel(repository = repository)

            val events = mutableListOf<MyListUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            advanceUntilIdle()

            assertEquals(1, repository.syncCallCount)
            assertEquals(0, events.size)
            assertFalse(viewModel.uiState.value.isSyncing)

            job.cancel()
        }

    @Test
    fun `onQuickIncrement updates episodes and shows completion prompt if total reached`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository()
            val viewModel = createViewModel(statusRepository = statusRepo)
            advanceUntilIdle()

            val anime =
                createAnime(
                    id = 10L,
                    title = "Almost Done",
                    status = UserAnimeStatus.WATCHING,
                    score = 8,
                    watchedEpisodes = 11,
                )

            val events = mutableListOf<MyListUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            viewModel.onQuickIncrement(anime)
            advanceUntilIdle()

            assertEquals(10L, statusRepo.lastUpdatedAnimeId)
            assertEquals(12, statusRepo.lastParams?.numWatchedEpisodes)

            val completionPrompt = events.firstOrNull { it is MyListUiEvent.ShowCompletionPrompt }
            assertNotNull(completionPrompt)
            val prompt = completionPrompt as MyListUiEvent.ShowCompletionPrompt
            assertEquals(10L, prompt.animeId)
            assertEquals("Almost Done", prompt.title)

            job.cancel()
        }

    @Test
    fun `edit sheet lifecycle - setEditingAnime, update, save, and dismiss`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository()
            val viewModel = createViewModel(statusRepository = statusRepo)

            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val anime = createAnime(10L, "Frieren", UserAnimeStatus.WATCHING, 10, 5)
            viewModel.setEditingAnime(anime)
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.editingSheetState)
            val sheet = viewModel.uiState.value.editingSheetState
            assertEquals(10L, sheet?.animeId)
            assertEquals(5, sheet?.episodesWatched)

            viewModel.updateEditingSheet { it.copy(episodesWatched = 6, score = 10) }
            advanceUntilIdle()
            assertEquals(
                6,
                viewModel.uiState.value.editingSheetState
                    ?.episodesWatched,
            )

            viewModel.onSaveEditSheet()
            advanceUntilIdle()

            assertEquals(10L, statusRepo.lastUpdatedAnimeId)
            assertEquals(6, statusRepo.lastParams?.numWatchedEpisodes)
            assertEquals(10, statusRepo.lastParams?.score)
            assertNull(viewModel.uiState.value.editingSheetState)

            collectJob.cancel()
        }

    @Test
    fun `onConfirmDeleteFromList deletes anime and dismisses sheet`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository()
            val viewModel = createViewModel(statusRepository = statusRepo)

            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val anime = createAnime(20L, "Drop Me", UserAnimeStatus.DROPPED, 3)
            viewModel.setEditingAnime(anime)
            advanceUntilIdle()

            viewModel.onConfirmDeleteFromList()
            advanceUntilIdle()

            assertEquals(20L, statusRepo.lastDeletedAnimeId)
            assertNull(viewModel.uiState.value.editingSheetState)

            collectJob.cancel()
        }

    @Test
    fun `onQuickIncrement with unknown or zero totalEpisodes does not show completion prompt`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository()
            val viewModel = createViewModel(statusRepository = statusRepo)
            advanceUntilIdle()

            val animeUnknown =
                createAnime(
                    id = 15L,
                    title = "One Piece",
                    status = UserAnimeStatus.WATCHING,
                    score = 9,
                    watchedEpisodes = 197,
                ).copy(totalEpisodes = 0)

            val events = mutableListOf<MyListUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            viewModel.onQuickIncrement(animeUnknown)
            advanceUntilIdle()

            assertEquals(15L, statusRepo.lastUpdatedAnimeId)
            assertEquals(198, statusRepo.lastParams?.numWatchedEpisodes)
            assertFalse(events.any { it is MyListUiEvent.ShowCompletionPrompt })

            job.cancel()
        }

    @Test
    fun `onSaveEditSheet for completed anime with reduced episodes shows watching prompt`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository()
            val viewModel = createViewModel(statusRepository = statusRepo)

            val events = mutableListOf<MyListUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            val completedAnime =
                createAnime(
                    id = 30L,
                    title = "Kino's Journey",
                    status = UserAnimeStatus.COMPLETED,
                    score = 9,
                    watchedEpisodes = 13,
                ).copy(totalEpisodes = 13)

            viewModel.setEditingAnime(completedAnime)
            advanceUntilIdle()

            viewModel.updateEditingSheet { it.copy(episodesWatched = 12) }
            advanceUntilIdle()

            viewModel.onSaveEditSheet()
            advanceUntilIdle()

            val prompt = events.firstOrNull { it is MyListUiEvent.ShowWatchingPrompt }
            assertNotNull(prompt)
            assertEquals(30L, (prompt as MyListUiEvent.ShowWatchingPrompt).animeId)
            assertEquals("Kino's Journey", prompt.title)

            job.cancel()
        }

    @Test
    fun `onMarkStatus updates anime status`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository()
            val viewModel = createViewModel(statusRepository = statusRepo)

            viewModel.onMarkStatus(42L, "watching")
            advanceUntilIdle()

            assertEquals(42L, statusRepo.lastUpdatedAnimeId)
            assertEquals("watching", statusRepo.lastParams?.status)
        }

    @Test
    fun `onMarkStatus failure emits error snackbar without retry action`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository().apply { shouldFailUpdate = true }
            val viewModel = createViewModel(statusRepository = statusRepo)

            val events = mutableListOf<MyListUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            viewModel.onMarkStatus(42L, "completed")
            advanceUntilIdle()

            val errorSnackbar =
                events.filterIsInstance<MyListUiEvent.ShowSnackbar>().find { it.isError }
            assertNotNull(errorSnackbar)
            assertEquals(CoreUiR.string.edit_list_status_update_error, errorSnackbar?.messageRes)
            assertNull(errorSnackbar?.actionRes)
            assertTrue(errorSnackbar?.isError == true)

            job.cancel()
        }

    @Test
    fun `onSaveEditSheet with score-only edit at final episode skips completion prompt`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository()
            val viewModel = createViewModel(statusRepository = statusRepo)

            val events = mutableListOf<MyListUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            val anime =
                createAnime(
                    id = 31L,
                    title = "Finished Series",
                    status = UserAnimeStatus.WATCHING,
                    score = 8,
                    watchedEpisodes = 12,
                ).copy(totalEpisodes = 12)

            viewModel.setEditingAnime(anime)
            advanceUntilIdle()

            viewModel.updateEditingSheet { it.copy(score = 9) }
            advanceUntilIdle()

            viewModel.onSaveEditSheet()
            advanceUntilIdle()

            assertFalse(events.any { it is MyListUiEvent.ShowCompletionPrompt })

            job.cancel()
        }

    @Test
    fun `onQuickIncrement does not update when watchedEpisodes reached totalEpisodes`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository()
            val viewModel = createViewModel(statusRepository = statusRepo)
            advanceUntilIdle()

            val animeFinished =
                createAnime(
                    id = 16L,
                    title = "Death Note",
                    status = UserAnimeStatus.WATCHING,
                    score = 10,
                    watchedEpisodes = 37,
                ).copy(totalEpisodes = 37)

            viewModel.onQuickIncrement(animeFinished)
            advanceUntilIdle()

            assertNull(statusRepo.lastUpdatedAnimeId)
        }

    @Test
    fun `onQuickIncrement failure emits snackbar without retry action`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository().apply { shouldFailUpdate = true }
            val viewModel = createViewModel(statusRepository = statusRepo)
            advanceUntilIdle()

            val anime =
                createAnime(
                    id = 17L,
                    title = "Steins;Gate",
                    status = UserAnimeStatus.WATCHING,
                    score = 10,
                    watchedEpisodes = 5,
                ).copy(totalEpisodes = 24)

            val events = mutableListOf<MyListUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            viewModel.onQuickIncrement(anime)
            advanceUntilIdle()

            val errorSnackbar =
                events.filterIsInstance<MyListUiEvent.ShowSnackbar>().find { it.isError }
            assertNotNull(errorSnackbar)
            assertEquals(CoreUiR.string.edit_list_status_update_error, errorSnackbar?.messageRes)
            assertNull(errorSnackbar?.actionRes)
            assertTrue(errorSnackbar?.isError == true)

            job.cancel()
        }

    @Test
    fun `onSaveEditSheet and onConfirmDeleteFromList guard against concurrent mutations`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository()
            val viewModel = createViewModel(statusRepository = statusRepo)

            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val anime = createAnime(25L, "Concurrent Test", UserAnimeStatus.WATCHING, 5)
            viewModel.setEditingAnime(anime)
            advanceUntilIdle()

            val deleteDeferred = CompletableDeferred<Result<Unit>>()
            statusRepo.deleteDeferred = deleteDeferred

            viewModel.onConfirmDeleteFromList()
            runCurrent()

            val sheet = viewModel.uiState.value.editingSheetState
            assertNotNull(sheet)
            assertTrue(sheet?.isDeleting == true)

            // Calling onSaveEditSheet while isDeleting is active must be ignored
            viewModel.onSaveEditSheet()
            runCurrent()

            assertNull(statusRepo.lastUpdatedAnimeId)

            // Calling onConfirmDeleteFromList a second time must also be ignored
            viewModel.onConfirmDeleteFromList()
            runCurrent()

            // Complete the delete operation
            deleteDeferred.complete(Result.success(Unit))
            advanceUntilIdle()

            assertEquals(25L, statusRepo.lastDeletedAnimeId)
            assertNull(viewModel.uiState.value.editingSheetState)

            collectJob.cancel()
        }

    @Test
    fun `inline increment and edit sheet guard against concurrent mutations`() =
        runTest {
            val statusRepo = FakeUserAnimeListStatusRepository()
            val viewModel = createViewModel(statusRepository = statusRepo)

            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val anime = createAnime(26L, "Inline Concurrent", UserAnimeStatus.WATCHING, 3)
            val updateDeferred = CompletableDeferred<Result<UserAnimeListUpdate>>()
            statusRepo.updateDeferred = updateDeferred

            viewModel.onQuickIncrement(anime)
            runCurrent()

            val state = viewModel.uiState.value
            val isSavingAnime = state.savingProgressAnimeIds.contains(26L)
            assertTrue(isSavingAnime)

            // Attempting to open edit sheet while inline increment is saving must be ignored
            viewModel.setEditingAnime(anime)
            runCurrent()

            assertNull(viewModel.uiState.value.editingSheetState)

            updateDeferred.complete(
                Result.success(
                    UserAnimeListUpdate(
                        animeId = 26L,
                        status = "watching",
                        score = 8,
                        numEpisodesWatched = 4,
                        isRewatching = false,
                        updatedAt = "2026-10-19T10:00:00Z",
                    ),
                ),
            )
            advanceUntilIdle()

            val updatedState = viewModel.uiState.value
            val isStillSaving = updatedState.savingProgressAnimeIds.contains(26L)
            assertFalse(isStillSaving)

            // Now edit sheet can be opened
            viewModel.setEditingAnime(anime)
            runCurrent()

            assertNotNull(viewModel.uiState.value.editingSheetState)

            // Calling onQuickIncrement while sheet is editing this anime must be ignored
            statusRepo.lastUpdatedAnimeId = null
            viewModel.onQuickIncrement(anime)
            runCurrent()

            assertNull(statusRepo.lastUpdatedAnimeId)

            collectJob.cancel()
        }

    private class FakeUserAnimeListStatusRepository : UserAnimeListStatusRepository {
        var lastUpdatedAnimeId: Long? = null
        var lastParams: UpdateUserAnimeStatusParams? = null
        var shouldFailUpdate = false
        var updateDeferred: CompletableDeferred<Result<UserAnimeListUpdate>>? = null

        var lastDeletedAnimeId: Long? = null
        var shouldFailDelete = false
        var deleteDeferred: CompletableDeferred<Result<Unit>>? = null

        override suspend fun updateAnimeStatus(
            animeId: Long,
            params: UpdateUserAnimeStatusParams,
        ): Result<UserAnimeListUpdate> {
            lastUpdatedAnimeId = animeId
            lastParams = params
            val deferred = updateDeferred
            return when {
                deferred != null -> deferred.await()
                shouldFailUpdate -> Result.failure(IOException("Update failed"))
                else ->
                    Result.success(
                        UserAnimeListUpdate(
                            animeId = animeId,
                            status = params.status ?: "watching",
                            score = params.score ?: 0,
                            numEpisodesWatched = params.numWatchedEpisodes ?: 0,
                            isRewatching = params.isRewatching ?: false,
                            updatedAt = "2026-10-19",
                        ),
                    )
            }
        }

        override suspend fun deleteAnimeFromList(animeId: Long): Result<Unit> {
            lastDeletedAnimeId = animeId
            val deferred = deleteDeferred
            return when {
                deferred != null -> deferred.await()
                shouldFailDelete -> Result.failure(IOException("Delete failed"))
                else -> Result.success(Unit)
            }
        }
    }
}
