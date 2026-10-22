package com.gokova.myanimelist.feature.details.presentation

import androidx.lifecycle.SavedStateHandle
import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.model.UserAnimeListUpdate
import com.gokova.myanimelist.core.domain.repository.UserAnimeListStatusRepository
import com.gokova.myanimelist.core.domain.usecase.DeleteAnimeFromListUseCase
import com.gokova.myanimelist.core.domain.usecase.IgnoredAnimeInteractor
import com.gokova.myanimelist.core.domain.usecase.ObserveIsAnimeNotInterestedUseCase
import com.gokova.myanimelist.core.domain.usecase.RemoveAnimeFromNotInterestedUseCase
import com.gokova.myanimelist.core.domain.usecase.SetAnimeNotInterestedUseCase
import com.gokova.myanimelist.core.domain.usecase.UpdateAnimeStatusUseCase
import com.gokova.myanimelist.core.ui.component.editlist.withStatus
import com.gokova.myanimelist.feature.details.R
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.domain.usecase.AddAnimeToMyListUseCase
import com.gokova.myanimelist.feature.details.domain.usecase.AnimeStatusInteractor
import com.gokova.myanimelist.feature.details.domain.usecase.ObserveAnimeDetailsUseCase
import com.gokova.myanimelist.feature.details.domain.usecase.RefreshAnimeDetailsUseCase
import com.gokova.myanimelist.feature.details.fakes.FakeAnimeDetailsRepository
import com.gokova.myanimelist.feature.details.fakes.FakeIgnoredAnimeRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import com.gokova.myanimelist.core.ui.R as CoreUiR

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeDetailsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAnimeDetailsRepository
    private lateinit var statusRepository: FakeUserAnimeListStatusRepository
    private lateinit var observeUseCase: ObserveAnimeDetailsUseCase
    private lateinit var refreshUseCase: RefreshAnimeDetailsUseCase
    private lateinit var addUseCase: AddAnimeToMyListUseCase
    private lateinit var updateUseCase: UpdateAnimeStatusUseCase
    private lateinit var deleteUseCase: DeleteAnimeFromListUseCase

    private lateinit var fakeIgnoredRepository: FakeIgnoredAnimeRepository
    private lateinit var statusInteractor: AnimeStatusInteractor
    private lateinit var ignoredAnimeInteractor: IgnoredAnimeInteractor

    private val testDetails =
        AnimeDetails(
            id = 42L,
            displayTitle = "Test Anime",
            subtitleTitle = null,
            originalTitle = "Test Anime",
            englishTitle = "Test Anime",
            mainPictureMedium = null,
            mainPictureLarge = null,
            synopsis = "A test synopsis",
            genres = listOf("Action"),
            themes = listOf("School"),
            meanScore = 8.0,
            userScore = null,
            userStatus = null,
            isInUserList = false,
            rank = 10,
            popularity = 5,
            numListUsers = 1000,
            numScoringUsers = 500,
            mediaType = "tv",
            status = "finished_airing",
            season = "2023 Spring",
            numEpisodes = 12,
            durationSeconds = 1440,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeAnimeDetailsRepository()
        statusRepository = FakeUserAnimeListStatusRepository()
        observeUseCase = ObserveAnimeDetailsUseCase(repository)
        refreshUseCase = RefreshAnimeDetailsUseCase(repository)
        addUseCase = AddAnimeToMyListUseCase(repository)
        updateUseCase = UpdateAnimeStatusUseCase(statusRepository)
        deleteUseCase = DeleteAnimeFromListUseCase(statusRepository)
        statusInteractor = AnimeStatusInteractor(updateUseCase, deleteUseCase)
        fakeIgnoredRepository = FakeIgnoredAnimeRepository()
        ignoredAnimeInteractor =
            IgnoredAnimeInteractor(
                observeIsIgnored = ObserveIsAnimeNotInterestedUseCase(fakeIgnoredRepository),
                setNotInterested = SetAnimeNotInterestedUseCase(fakeIgnoredRepository),
                removeNotInterested = RemoveAnimeFromNotInterestedUseCase(fakeIgnoredRepository),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(animeId: Long = 42L): AnimeDetailsViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("animeId" to animeId))
        return AnimeDetailsViewModel(
            savedStateHandle = savedStateHandle,
            observeAnimeDetailsUseCase = observeUseCase,
            refreshAnimeDetailsUseCase = refreshUseCase,
            addAnimeToMyListUseCase = addUseCase,
            statusInteractor = statusInteractor,
            ignoredAnimeInteractor = ignoredAnimeInteractor,
        )
    }

    @Test
    fun `initialization loads cached details from observation and completes refresh`() =
        runTest {
            repository.emitDetails(testDetails)
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isRefreshing)
            assertEquals(testDetails, state.details)
        }

    @Test
    fun `initialization when no cache triggers loading then populates details`() =
        runTest {
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(testDetails, state.details)
            assertNull(state.errorMessageRes)
        }

    @Test
    fun `initialization failure with no cache sets error state and allows retry`() =
        runTest {
            repository.fetchResult = Result.failure(RuntimeException("Network error"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.details)
            assertEquals(R.string.details_error_not_found, state.errorMessageRes)
            assertTrue(state.canRetry)
        }

    @Test
    fun `initialization failure with cache shows cached details and sends snackbar event`() =
        runTest {
            repository.emitDetails(testDetails)
            repository.fetchResult = Result.failure(RuntimeException("Offline"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(testDetails, state.details)

            val event = viewModel.events.first()
            assertTrue(event is AnimeDetailsUiEvent.ShowSnackbar)
            assertEquals(
                R.string.details_offline_showing_cached,
                (event as AnimeDetailsUiEvent.ShowSnackbar).messageRes,
            )
        }

    @Test
    fun `onRefresh performs refresh and updates isRefreshing state`() =
        runTest {
            repository.emitDetails(testDetails)
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val updatedDetails = testDetails.copy(meanScore = 9.5)
            repository.fetchResult = Result.success(updatedDetails)

            viewModel.onRefresh()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertEquals(9.5, state.details?.meanScore)
        }

    @Test
    fun `onRetry reloads details when in error state`() =
        runTest {
            repository.fetchResult = Result.failure(RuntimeException("Initial failure"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.canRetry)

            repository.fetchResult = Result.success(testDetails)
            viewModel.onRetry()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.errorMessageRes)
            assertEquals(testDetails, state.details)
        }

    @Test
    fun `onAddToList success updates in user list and sends snackbar event`() =
        runTest {
            repository.emitDetails(testDetails)
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAddToList()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isAddingToList)
            assertTrue(state.details?.isInUserList == true)
            assertEquals("plan_to_watch", state.details?.userStatus)

            val event = viewModel.events.first()
            assertTrue(event is AnimeDetailsUiEvent.ShowSnackbar)
            assertEquals(
                R.string.details_added_to_plan_to_watch,
                (event as AnimeDetailsUiEvent.ShowSnackbar).messageRes,
            )
        }

    @Test
    fun `onAddToList failure sends error snackbar event`() =
        runTest {
            repository.emitDetails(testDetails)
            repository.fetchResult = Result.success(testDetails)
            repository.addResult = Result.failure(RuntimeException("Server 500"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAddToList()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isAddingToList)
            assertFalse(state.details?.isInUserList == true)

            val event = viewModel.events.first() as AnimeDetailsUiEvent.ShowSnackbar
            assertEquals(
                R.string.details_add_to_list_failed,
                event.messageRes,
            )
            assertTrue(event.isError)
        }

    @Test
    fun `setEditingSheet opens and closes bottom sheet state`() =
        runTest {
            val watchingDetails = testDetails.copy(isInUserList = true, userStatus = "watching")
            repository.emitDetails(watchingDetails)
            repository.fetchResult = Result.success(watchingDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEditingSheet(open = true)
            advanceUntilIdle()

            val sheet = viewModel.uiState.value.editingSheetState
            assertNotNull(sheet)
            assertEquals(42L, sheet?.animeId)
            assertEquals("watching", sheet?.status)

            viewModel.setEditingSheet(open = false)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.editingSheetState)
        }

    @Test
    fun `onSaveEditSheet updates details and dismisses sheet`() =
        runTest {
            repository.emitDetails(
                testDetails.copy(
                    isInUserList = true,
                    userStatus = "watching",
                    numWatchedEpisodes = 2,
                ),
            )
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEditingSheet(open = true)
            advanceUntilIdle()

            viewModel.updateEditingSheet {
                it.copy(
                    status = "completed",
                    episodesWatched = 12,
                    score = 9,
                    isRewatching = false,
                )
            }
            viewModel.onSaveEditSheet()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.editingSheetState)
            assertEquals("completed", state.details?.userStatus)
            assertEquals(9, state.details?.userScore)
            assertEquals(12, state.details?.numWatchedEpisodes)
            assertEquals(42L, statusRepository.lastUpdatedAnimeId)
            assertEquals(9, statusRepository.lastParams?.score)
            assertEquals(12, statusRepository.lastParams?.numWatchedEpisodes)
        }

    @Test
    fun `onSaveEditSheet reaching total emits ShowCompletionPrompt when not completed`() =
        runTest {
            repository.emitDetails(
                testDetails.copy(
                    isInUserList = true,
                    userStatus = "watching",
                    numWatchedEpisodes = 10,
                ),
            )
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEditingSheet(open = true)
            advanceUntilIdle()

            viewModel.updateEditingSheet {
                it.copy(
                    status = "watching",
                    episodesWatched = 12,
                )
            }
            viewModel.onSaveEditSheet()
            advanceUntilIdle()

            val event = viewModel.events.first()
            assertTrue(event is AnimeDetailsUiEvent.ShowCompletionPrompt)
            assertEquals(42L, (event as AnimeDetailsUiEvent.ShowCompletionPrompt).animeId)
        }

    @Test
    fun `onSaveEditSheet with unknown total episodes does not emit ShowCompletionPrompt`() =
        runTest {
            val ongoingDetails =
                testDetails.copy(
                    isInUserList = true,
                    userStatus = "watching",
                    numWatchedEpisodes = 100,
                    numEpisodes = 0,
                )
            repository.emitDetails(ongoingDetails)
            repository.fetchResult = Result.success(ongoingDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEditingSheet(open = true)
            advanceUntilIdle()

            viewModel.updateEditingSheet {
                it.copy(
                    status = "watching",
                    episodesWatched = 101,
                )
            }
            viewModel.onSaveEditSheet()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.editingSheetState)
            assertEquals(101, state.details?.numWatchedEpisodes)
        }

    @Test
    fun `updateEditingSheet withStatus completed sets episodes to total when total is known`() =
        runTest {
            val details =
                testDetails.copy(
                    isInUserList = true,
                    userStatus = "watching",
                    numWatchedEpisodes = 5,
                    numEpisodes = 24,
                )
            repository.emitDetails(details)
            repository.fetchResult = Result.success(details)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEditingSheet(open = true)
            advanceUntilIdle()

            viewModel.updateEditingSheet { it.withStatus("completed") }

            val sheet = viewModel.uiState.value.editingSheetState
            assertNotNull(sheet)
            assertEquals("completed", sheet?.status)
            assertEquals(24, sheet?.episodesWatched)
        }

    @Test
    fun `updateEditingSheet withStatus completed preserves episodes when total is zero`() =
        runTest {
            val ongoingDetails =
                testDetails.copy(
                    isInUserList = true,
                    userStatus = "watching",
                    numWatchedEpisodes = 50,
                    numEpisodes = 0,
                )
            repository.emitDetails(ongoingDetails)
            repository.fetchResult = Result.success(ongoingDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEditingSheet(open = true)
            advanceUntilIdle()

            viewModel.updateEditingSheet { it.withStatus("completed") }

            val sheet = viewModel.uiState.value.editingSheetState
            assertNotNull(sheet)
            assertEquals("completed", sheet?.status)
            assertEquals(50, sheet?.episodesWatched)
        }

    @Test
    fun `onSaveEditSheet reducing episodes on completed anime emits ShowWatchingPrompt`() =
        runTest {
            val completedDetails =
                testDetails.copy(
                    isInUserList = true,
                    userStatus = "completed",
                    numWatchedEpisodes = 12,
                )
            repository.emitDetails(completedDetails)
            repository.fetchResult = Result.success(completedDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEditingSheet(open = true)
            advanceUntilIdle()

            viewModel.updateEditingSheet {
                it.copy(
                    status = "completed",
                    episodesWatched = 10,
                )
            }
            viewModel.onSaveEditSheet()
            advanceUntilIdle()

            val event = viewModel.events.first()
            assertTrue(event is AnimeDetailsUiEvent.ShowWatchingPrompt)
            assertEquals(42L, (event as AnimeDetailsUiEvent.ShowWatchingPrompt).animeId)
        }

    @Test
    fun `onMarkStatus updates status and refreshes details userStatus`() =
        runTest {
            repository.emitDetails(
                testDetails.copy(
                    isInUserList = true,
                    userStatus = "watching",
                    numWatchedEpisodes = 12,
                ),
            )
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onMarkStatus("completed")
            advanceUntilIdle()

            assertEquals(
                "completed",
                viewModel.uiState.value.details
                    ?.userStatus,
            )
            assertEquals(42L, statusRepository.lastUpdatedAnimeId)
            assertEquals("completed", statusRepository.lastParams?.status)
        }

    @Test
    fun `onMarkStatus failure emits ShowSnackbar with isError true`() =
        runTest {
            statusRepository.shouldFailUpdate = true
            repository.emitDetails(
                testDetails.copy(
                    isInUserList = true,
                    userStatus = "watching",
                    numWatchedEpisodes = 12,
                ),
            )
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onMarkStatus("completed")
            advanceUntilIdle()

            val event = viewModel.events.first()
            assertTrue(event is AnimeDetailsUiEvent.ShowSnackbar)
            val snackbar = event as AnimeDetailsUiEvent.ShowSnackbar
            assertEquals(CoreUiR.string.edit_list_status_update_error, snackbar.messageRes)
            assertTrue(snackbar.isError)
        }

    @Test
    fun `onSaveEditSheet with score-only edit at final episode skips completion prompt`() =
        runTest {
            val completedDetails =
                testDetails.copy(
                    isInUserList = true,
                    userStatus = "watching",
                    numWatchedEpisodes = 12,
                    numEpisodes = 12,
                )
            repository.emitDetails(completedDetails)
            repository.fetchResult = Result.success(completedDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEditingSheet(open = true)
            advanceUntilIdle()

            viewModel.updateEditingSheet { it.copy(score = 10) }
            viewModel.onSaveEditSheet()
            advanceUntilIdle()

            val events = mutableListOf<AnimeDetailsUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }
            runCurrent()

            assertFalse(events.any { it is AnimeDetailsUiEvent.ShowCompletionPrompt })

            job.cancel()
        }

    @Test
    fun `onConfirmDeleteFromList deletes anime and clears user list status`() =
        runTest {
            repository.emitDetails(testDetails.copy(isInUserList = true, userStatus = "watching"))
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEditingSheet(open = true)
            advanceUntilIdle()

            viewModel.onConfirmDeleteFromList()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.editingSheetState)
            assertFalse(state.details?.isInUserList == true)
            assertNull(state.details?.userStatus)
            assertNull(state.details?.userScore)
            assertEquals(0, state.details?.numWatchedEpisodes)
            assertEquals(42L, statusRepository.lastDeletedAnimeId)
        }

    @Test
    fun `onAnimeClick emits NavigateToDetails event`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onAnimeClick(1234L)
            advanceUntilIdle()

            val event = viewModel.events.first()
            assertTrue(event is AnimeDetailsUiEvent.NavigateToDetails)
            assertEquals(1234L, (event as AnimeDetailsUiEvent.NavigateToDetails).animeId)
        }

    @Test
    fun `onBackClick emits NavigateBack event`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onBackClick()
            advanceUntilIdle()

            val event = viewModel.events.first()
            assertEquals(AnimeDetailsUiEvent.NavigateBack, event)
        }

    @Test
    fun `onSaveEditSheet and onConfirmDeleteFromList guard against concurrent mutations`() =
        runTest {
            repository.emitDetails(testDetails.copy(isInUserList = true, userStatus = "watching"))
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEditingSheet(open = true)
            advanceUntilIdle()

            val deleteDeferred = CompletableDeferred<Result<Unit>>()
            statusRepository.deleteDeferred = deleteDeferred

            viewModel.onConfirmDeleteFromList()
            runCurrent()

            val sheet = viewModel.uiState.value.editingSheetState
            assertNotNull(sheet)
            assertTrue(sheet?.isDeleting == true)

            // Calling onSaveEditSheet while isDeleting is active must be ignored
            viewModel.onSaveEditSheet()
            runCurrent()

            assertNull(statusRepository.lastUpdatedAnimeId)

            // Calling onConfirmDeleteFromList a second time must also be ignored
            viewModel.onConfirmDeleteFromList()
            runCurrent()

            // Complete the delete operation
            deleteDeferred.complete(Result.success(Unit))
            advanceUntilIdle()

            assertEquals(42L, statusRepository.lastDeletedAnimeId)
            assertNull(viewModel.uiState.value.editingSheetState)
        }

    @Test
    fun `onToggleNotInterested marks anime as not interested when not ignored`() =
        runTest {
            repository.emitDetails(testDetails)
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isNotInterested)

            viewModel.onToggleNotInterested()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isNotInterested)
            assertTrue(fakeIgnoredRepository.isIgnored(42L))
            val event = viewModel.events.first()
            assertTrue(event is AnimeDetailsUiEvent.ShowSnackbar)
            assertEquals(
                R.string.details_snackbar_marked_not_interested,
                (event as AnimeDetailsUiEvent.ShowSnackbar).messageRes,
            )
        }

    @Test
    fun `onToggleNotInterested removes anime from not interested when already ignored`() =
        runTest {
            fakeIgnoredRepository.setAnimeNotInterested(42L, "Test Anime")
            repository.emitDetails(testDetails)
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isNotInterested)

            viewModel.onToggleNotInterested()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isNotInterested)
            assertFalse(fakeIgnoredRepository.isIgnored(42L))
            val event = viewModel.events.first()
            assertTrue(event is AnimeDetailsUiEvent.ShowSnackbar)
            assertEquals(
                R.string.details_snackbar_removed_not_interested,
                (event as AnimeDetailsUiEvent.ShowSnackbar).messageRes,
            )
        }

    @Test
    fun `onToggleNotInterested rapid consecutive calls ignore second call while in flight`() =
        runTest {
            repository.emitDetails(testDetails)
            repository.fetchResult = Result.success(testDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val events = mutableListOf<AnimeDetailsUiEvent>()
            val job = launch { viewModel.events.collect { events.add(it) } }

            viewModel.onToggleNotInterested()
            viewModel.onToggleNotInterested()
            advanceUntilIdle()

            job.cancel()

            assertTrue(viewModel.uiState.value.isNotInterested)
            assertTrue(fakeIgnoredRepository.isIgnored(42L))
            assertEquals(1, events.size)
            assertTrue(events.first() is AnimeDetailsUiEvent.ShowSnackbar)
        }

    @Test
    fun `onToggleNotInterested does nothing when details are null`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onToggleNotInterested()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isNotInterested)
            assertFalse(fakeIgnoredRepository.isIgnored(42L))
        }

    @Test
    fun `onToggleNotInterested does nothing when anime is in user list`() =
        runTest {
            val inListDetails = testDetails.copy(isInUserList = true)
            repository.emitDetails(inListDetails)
            repository.fetchResult = Result.success(inListDetails)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onToggleNotInterested()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isNotInterested)
            assertFalse(fakeIgnoredRepository.isIgnored(42L))
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
