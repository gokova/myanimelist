package com.gokova.myanimelist.feature.details.presentation

import androidx.lifecycle.SavedStateHandle
import com.gokova.myanimelist.feature.details.R
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.domain.model.DetailRecommendation
import com.gokova.myanimelist.feature.details.domain.model.RelatedAnime
import com.gokova.myanimelist.feature.details.domain.usecase.AddAnimeToMyListUseCase
import com.gokova.myanimelist.feature.details.domain.usecase.ObserveAnimeDetailsUseCase
import com.gokova.myanimelist.feature.details.domain.usecase.RefreshAnimeDetailsUseCase
import com.gokova.myanimelist.feature.details.fakes.FakeAnimeDetailsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeDetailsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAnimeDetailsRepository
    private lateinit var observeUseCase: ObserveAnimeDetailsUseCase
    private lateinit var refreshUseCase: RefreshAnimeDetailsUseCase
    private lateinit var addUseCase: AddAnimeToMyListUseCase

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
        observeUseCase = ObserveAnimeDetailsUseCase(repository)
        refreshUseCase = RefreshAnimeDetailsUseCase(repository)
        addUseCase = AddAnimeToMyListUseCase(repository)
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
    fun `local observation emission preserves rich relations when incoming has empty lists`() =
        runTest {
            val richDetails =
                testDetails.copy(
                    subtitleTitle = "Japanese Subtitle",
                    numScoringUsers = 1000,
                    relatedAnime =
                        listOf(
                            RelatedAnime(
                                id = 999L,
                                title = "Related Title",
                                thumbnailUrl = null,
                                relationType = "sequel",
                                relationTypeFormatted = "Sequel",
                            ),
                        ),
                    recommendations =
                        listOf(
                            DetailRecommendation(
                                id = 888L,
                                title = "Rec Title",
                                thumbnailUrl = null,
                                meanScore = 8.5,
                                numRecommendations = 10,
                            ),
                        ),
                )
            val bareDetails =
                testDetails.copy(
                    subtitleTitle = null,
                    numScoringUsers = null,
                    relatedAnime = emptyList(),
                    recommendations = emptyList(),
                )

            repository.fetchResult = Result.success(richDetails)
            val viewModel = createViewModel()
            advanceUntilIdle()

            val initialDetails = viewModel.uiState.value.details
            assertEquals(1, initialDetails?.relatedAnime?.size)
            assertEquals("Japanese Subtitle", initialDetails?.subtitleTitle)

            repository.emitDetails(bareDetails)
            advanceUntilIdle()

            val finalDetails = viewModel.uiState.value.details
            assertEquals(1, finalDetails?.relatedAnime?.size)
            assertEquals(1, finalDetails?.recommendations?.size)
            assertEquals(1000, finalDetails?.numScoringUsers)
            assertEquals("Japanese Subtitle", finalDetails?.subtitleTitle)
        }

    @Test
    fun `refresh failure with cached details shows snackbar and keeps cached details`() =
        runTest {
            repository.emitDetails(testDetails)
            repository.fetchResult = Result.failure(RuntimeException("No internet"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isRefreshing)
            assertNotNull(state.details)

            val event = viewModel.events.first()
            assertTrue(event is AnimeDetailsUiEvent.ShowSnackbar)
            assertEquals(
                R.string.details_offline_showing_cached,
                (event as AnimeDetailsUiEvent.ShowSnackbar).messageRes,
            )
        }

    @Test
    fun `refresh failure without cached details shows error state and enables retry`() =
        runTest {
            repository.fetchResult = Result.failure(RuntimeException("Network error"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(R.string.details_error_not_found, state.errorMessageRes)
            assertTrue(state.canRetry)
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
}
