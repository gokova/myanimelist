package com.gokova.myanimelist.feature.recommendation.presentation

import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationEngineState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime
import com.gokova.myanimelist.feature.recommendation.domain.repository.NewSeasonRepository
import com.gokova.myanimelist.feature.recommendation.domain.repository.RecommendationRepository
import com.gokova.myanimelist.feature.recommendation.domain.usecase.NewSeasonsInteractor
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ObserveNewSeasonStateUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ObserveNewSeasonsUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ObserveRecommendationStateUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ObserveRecommendationsUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ScheduleNewSeasonWorkUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ScheduleRecommendationWorkUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.TriggerNewSeasonCalculationUseCase
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
    private val fakeNewSeasonRepository = FakeNewSeasonRepository()

    private class FakeRecommendationRepository : RecommendationRepository {
        val stateFlow =
            MutableStateFlow<RecommendationEngineState>(RecommendationEngineState.Ready(1))
        val genreFlow = MutableStateFlow<List<RecommendedAnime>>(emptyList())
        val themeFlow = MutableStateFlow<List<RecommendedAnime>>(emptyList())
        var triggerCount = 0
        var scheduleCount = 0

        override fun observeRecommendations(
            type: RecommendationType,
        ): Flow<List<RecommendedAnime>> =
            when (type) {
                RecommendationType.GENRE -> genreFlow
                RecommendationType.THEME -> themeFlow
                RecommendationType.NEW_SEASONS -> MutableStateFlow(emptyList())
            }

        override fun observeEngineState(): Flow<RecommendationEngineState> = stateFlow

        override suspend fun scheduleInitialOrPeriodicWork() {
            scheduleCount++
        }

        override suspend fun triggerImmediateEvaluation() {
            triggerCount++
        }
    }

    private class FakeNewSeasonRepository : NewSeasonRepository {
        val stateFlow = MutableStateFlow<NewSeasonState>(NewSeasonState.Ready(1))
        val newSeasonsFlow = MutableStateFlow<List<NewSeasonAnime>>(emptyList())
        var triggerCount = 0
        var scheduleCount = 0
        var lastSortOption: NewSeasonSortOption? = null

        override fun observeNewSeasons(
            sortOption: NewSeasonSortOption,
        ): Flow<List<NewSeasonAnime>> {
            lastSortOption = sortOption
            return newSeasonsFlow
        }

        override fun observeNewSeasonState(): Flow<NewSeasonState> = stateFlow

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

        val newSeasonsInteractor =
            NewSeasonsInteractor(
                observeNewSeasons = ObserveNewSeasonsUseCase(fakeNewSeasonRepository),
                observeNewSeasonState = ObserveNewSeasonStateUseCase(fakeNewSeasonRepository),
                scheduleWork = ScheduleNewSeasonWorkUseCase(fakeNewSeasonRepository),
                triggerCalculation = TriggerNewSeasonCalculationUseCase(fakeNewSeasonRepository),
            )

        return RecommendationViewModel(
            observeRecommendationsUseCase = observeRecs,
            observeRecommendationStateUseCase = observeState,
            triggerCalculationUseCase = triggerCalc,
            scheduleRecommendationWorkUseCase = scheduleWork,
            newSeasons = newSeasonsInteractor,
        )
    }

    private fun sampleAnime(
        id: Long,
        title: String,
    ) = RecommendedAnime(
        animeId = id,
        title = title,
        titleEnglish = null,
        thumbnailUrl = null,
        largeImageUrl = null,
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

    @Suppress("SameParameterValue")
    private fun sampleNewSeason(
        id: Long,
        title: String,
    ) = NewSeasonAnime(
        animeId = id,
        displayTitle = title,
        subtitleTitle = null,
        thumbnailUrl = null,
        largeImageUrl = null,
        mediaType = "TV",
        releaseSeason = "2026 Summer",
        airingStatus = "finished_airing",
        score = 8.5,
        totalEpisodes = 12,
        parentAnimeId = 999L,
        parentTitle = "Parent Anime",
        relationType = "sequel",
        relationTypeFormatted = "Sequel",
        startSeasonYear = 2026,
        startSeasonSeason = "summer",
    )

    @Test
    fun `viewModel schedules periodic work on init`() =
        runTest {
            createViewModel()
            testScheduler.advanceUntilIdle()
            assertEquals(1, fakeRepository.scheduleCount)
            assertEquals(1, fakeNewSeasonRepository.scheduleCount)
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
    fun `selecting new seasons tab emits Success with new seasons and default sort`() =
        runTest {
            val seasons = listOf(sampleNewSeason(100L, "New Season 1"))
            fakeNewSeasonRepository.newSeasonsFlow.value = seasons

            val viewModel = createViewModel()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()
            viewModel.onEvent(RecommendationUiEvent.SelectType(RecommendationType.NEW_SEASONS))
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is RecommendationUiState.Success)
            val success = state as RecommendationUiState.Success
            assertEquals(RecommendationType.NEW_SEASONS, success.selectedType)
            assertEquals(seasons, success.newSeasons)
            assertEquals(NewSeasonSortOption.RELEASE_DATE_DESC, success.newSeasonSort)

            collectJob.cancel()
        }

    @Test
    fun `selecting new season sort option updates sort and emits sorted new seasons`() =
        runTest {
            val seasons = listOf(sampleNewSeason(100L, "New Season 1"))
            fakeNewSeasonRepository.newSeasonsFlow.value = seasons

            val viewModel = createViewModel()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()
            viewModel.onEvent(RecommendationUiEvent.SelectType(RecommendationType.NEW_SEASONS))
            testScheduler.advanceUntilIdle()

            viewModel.onEvent(
                RecommendationUiEvent.SelectNewSeasonSort(NewSeasonSortOption.SCORE_DESC),
            )
            testScheduler.advanceUntilIdle()

            assertEquals(NewSeasonSortOption.SCORE_DESC, fakeNewSeasonRepository.lastSortOption)
            val state = viewModel.uiState.value as RecommendationUiState.Success
            assertEquals(NewSeasonSortOption.SCORE_DESC, state.newSeasonSort)

            collectJob.cancel()
        }

    @Test
    fun `calculateNow triggers immediate calculation for genres when genre active`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onEvent(RecommendationUiEvent.CalculateNow)
            testScheduler.advanceUntilIdle()

            assertEquals(1, fakeRepository.triggerCount)
            assertEquals(0, fakeNewSeasonRepository.triggerCount)
        }

    @Test
    fun `calculateNow triggers new seasons calculation when new seasons active`() =
        runTest {
            val viewModel = createViewModel()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()
            viewModel.onEvent(RecommendationUiEvent.SelectType(RecommendationType.NEW_SEASONS))
            testScheduler.advanceUntilIdle()

            viewModel.onEvent(RecommendationUiEvent.CalculateNow)
            testScheduler.advanceUntilIdle()

            assertEquals(0, fakeRepository.triggerCount)
            assertEquals(1, fakeNewSeasonRepository.triggerCount)

            collectJob.cancel()
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
            assertEquals(RecommendationUiState.EmptyInsufficientData(), viewModel.uiState.value)

            collectJob.cancel()
        }

    @Test
    fun `emits EmptyAllCaughtUp when new seasons state is EmptyAllCaughtUp`() =
        runTest {
            fakeNewSeasonRepository.stateFlow.value = NewSeasonState.EmptyAllCaughtUp
            val viewModel = createViewModel()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()
            viewModel.onEvent(RecommendationUiEvent.SelectType(RecommendationType.NEW_SEASONS))
            testScheduler.advanceUntilIdle()

            assertEquals(RecommendationUiState.EmptyAllCaughtUp(), viewModel.uiState.value)

            collectJob.cancel()
        }

    @Test
    fun `emits EmptyAllCaughtUp when new seasons state is Ready but list is empty`() =
        runTest {
            fakeNewSeasonRepository.stateFlow.value = NewSeasonState.Ready(0)
            fakeNewSeasonRepository.newSeasonsFlow.value = emptyList()

            val viewModel = createViewModel()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()
            viewModel.onEvent(RecommendationUiEvent.SelectType(RecommendationType.NEW_SEASONS))
            testScheduler.advanceUntilIdle()

            assertEquals(RecommendationUiState.EmptyAllCaughtUp(), viewModel.uiState.value)

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
            assertEquals(RecommendationUiState.Calculating(), viewModel.uiState.value)

            collectJob.cancel()
        }

    @Test
    fun `when engine state transitions to Calculating, does not re-trigger schedule work`() =
        runTest {
            val viewModel = createViewModel()
            testScheduler.advanceUntilIdle()
            val initialScheduleCount = fakeRepository.scheduleCount

            fakeRepository.stateFlow.value = RecommendationEngineState.Calculating
            testScheduler.advanceUntilIdle()

            assertEquals(initialScheduleCount, fakeRepository.scheduleCount)
        }

    @Test
    fun `switching tab from new seasons calculating state switches selectedType and emits recs`() =
        runTest {
            val genreAnime = listOf(sampleAnime(1L, "Genre 1"))
            fakeRepository.genreFlow.value = genreAnime
            fakeNewSeasonRepository.stateFlow.value = NewSeasonState.Calculating

            val viewModel = createViewModel()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()
            viewModel.onEvent(RecommendationUiEvent.SelectType(RecommendationType.NEW_SEASONS))
            testScheduler.advanceUntilIdle()

            val calculatingState = viewModel.uiState.value
            assertEquals(
                RecommendationUiState.Calculating(RecommendationType.NEW_SEASONS),
                calculatingState,
            )
            assertEquals(RecommendationType.NEW_SEASONS, calculatingState.selectedType)

            viewModel.onEvent(RecommendationUiEvent.SelectType(RecommendationType.GENRE))
            testScheduler.advanceUntilIdle()

            val genreState = viewModel.uiState.value
            assertTrue(genreState is RecommendationUiState.Success)
            assertEquals(RecommendationType.GENRE, genreState.selectedType)
            val success = genreState as RecommendationUiState.Success
            assertEquals(genreAnime, success.recommendations)

            collectJob.cancel()
        }

    @Test
    fun `switching tab from all caught up state switches selectedType and emits recs`() =
        runTest {
            val genreAnime = listOf(sampleAnime(1L, "Genre 1"))
            fakeRepository.genreFlow.value = genreAnime
            fakeNewSeasonRepository.stateFlow.value = NewSeasonState.EmptyAllCaughtUp

            val viewModel = createViewModel()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()
            viewModel.onEvent(RecommendationUiEvent.SelectType(RecommendationType.NEW_SEASONS))
            testScheduler.advanceUntilIdle()

            val emptyState = viewModel.uiState.value
            assertEquals(RecommendationUiState.EmptyAllCaughtUp(), emptyState)
            assertEquals(RecommendationType.NEW_SEASONS, emptyState.selectedType)

            viewModel.onEvent(RecommendationUiEvent.SelectType(RecommendationType.GENRE))
            testScheduler.advanceUntilIdle()

            val genreState = viewModel.uiState.value
            assertTrue(genreState is RecommendationUiState.Success)
            assertEquals(RecommendationType.GENRE, genreState.selectedType)
            val success = genreState as RecommendationUiState.Success
            assertEquals(genreAnime, success.recommendations)

            collectJob.cancel()
        }
}
