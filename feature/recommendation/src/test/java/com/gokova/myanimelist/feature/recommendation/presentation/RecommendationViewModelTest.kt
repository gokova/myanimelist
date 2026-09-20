package com.gokova.myanimelist.feature.recommendation.presentation

import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationEngineState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime
import com.gokova.myanimelist.feature.recommendation.domain.repository.RecommendationRepository
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ObserveRecommendationStateUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ObserveRecommendationsUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ScheduleRecommendationWorkUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.TriggerRecommendationCalculationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeRecommendationRepository()

    private class FakeRecommendationRepository : RecommendationRepository {
        val stateFlow =
            MutableStateFlow<RecommendationEngineState>(RecommendationEngineState.Ready(1))
        val genreFlow = MutableStateFlow<List<RecommendedAnime>>(emptyList())
        val themeFlow = MutableStateFlow<List<RecommendedAnime>>(emptyList())
        var triggerCount = 0
        var scheduleCount = 0

        override fun observeRecommendations(type: RecommendationType) =
            when (type) {
                RecommendationType.GENRE -> genreFlow
                RecommendationType.THEME -> themeFlow
            }

        override fun observeEngineState(): Flow<RecommendationEngineState> = stateFlow

        override suspend fun scheduleInitialOrPeriodicWork() {
            scheduleCount++
        }

        override suspend fun triggerImmediateEvaluation() {
            triggerCount++
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RecommendationViewModel {
        val observeRecs = ObserveRecommendationsUseCase(fakeRepository)
        val observeState = ObserveRecommendationStateUseCase(fakeRepository)
        val triggerCalc = TriggerRecommendationCalculationUseCase(fakeRepository)
        val scheduleWork = ScheduleRecommendationWorkUseCase(fakeRepository)
        return RecommendationViewModel(observeRecs, observeState, triggerCalc, scheduleWork)
    }

    private fun sampleAnime(
        id: Long,
        title: String,
    ) = RecommendedAnime(
        animeId = id,
        title = title,
        titleEnglish = null,
        imageUrl = null,
        mediaType = "tv",
        airingStatus = "finished_airing",
        numEpisodes = 12,
        startSeasonYear = 2026,
        startSeasonSeason = "summer",
        meanScore = 8.5,
        genreRank = 1,
        genreScore = 5.0,
        genreMatchPercent = 95,
        themeRank = 1,
        themeScore = 5.0,
        themeMatchPercent = 95,
    )

    @Test
    fun `viewModel schedules periodic work on init`() =
        runTest {
            createViewModel()
            testScheduler.advanceUntilIdle()
            assertEquals(1, fakeRepository.scheduleCount)
        }

    @Test
    fun `uiState emits Success when engine is Ready and recommendations exist`() =
        runTest {
            val anime = listOf(sampleAnime(1L, "Anime 1"))
            fakeRepository.genreFlow.value = anime

            val viewModel = createViewModel()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is RecommendationUiState.Success)
            assertEquals(anime, (state as RecommendationUiState.Success).recommendations)
            assertEquals(RecommendationType.GENRE, state.selectedType)

            collectJob.cancel()
        }

    @Test
    fun `selecting theme type switches emitted recommendations`() =
        runTest {
            val genreAnime = listOf(sampleAnime(1L, "Genre 1"))
            val themeAnime = listOf(sampleAnime(2L, "Theme 2"))
            fakeRepository.genreFlow.value = genreAnime
            fakeRepository.themeFlow.value = themeAnime

            val viewModel = createViewModel()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()
            viewModel.onEvent(RecommendationUiEvent.SelectType(RecommendationType.THEME))
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is RecommendationUiState.Success)
            assertEquals(themeAnime, (state as RecommendationUiState.Success).recommendations)
            assertEquals(RecommendationType.THEME, state.selectedType)

            collectJob.cancel()
        }

    @Test
    fun `calculateNow triggers immediate calculation`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onEvent(RecommendationUiEvent.CalculateNow)
            testScheduler.advanceUntilIdle()

            assertEquals(1, fakeRepository.triggerCount)
        }

    @Test
    fun `emits EmptyInsufficientData when engine state is EmptyInsufficientData`() =
        runTest {
            fakeRepository.stateFlow.value = RecommendationEngineState.EmptyInsufficientData
            val viewModel = createViewModel()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()
            assertEquals(RecommendationUiState.EmptyInsufficientData, viewModel.uiState.value)

            collectJob.cancel()
        }

    @Test
    fun `emits Calculating when engine state is Calculating`() =
        runTest {
            fakeRepository.stateFlow.value = RecommendationEngineState.Calculating
            val viewModel = createViewModel()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()
            assertEquals(RecommendationUiState.Calculating, viewModel.uiState.value)

            collectJob.cancel()
        }

    @Test
    fun `when engine state transitions to Calculating, triggers schedule work`() =
        runTest {
            val viewModel = createViewModel()
            testScheduler.advanceUntilIdle()
            val initialScheduleCount = fakeRepository.scheduleCount

            fakeRepository.stateFlow.value = RecommendationEngineState.Calculating
            testScheduler.advanceUntilIdle()

            assertEquals(initialScheduleCount + 1, fakeRepository.scheduleCount)
        }
}
