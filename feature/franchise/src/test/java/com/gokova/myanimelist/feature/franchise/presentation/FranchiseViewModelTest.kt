package com.gokova.myanimelist.feature.franchise.presentation

import androidx.lifecycle.SavedStateHandle
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraph
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNode
import com.gokova.myanimelist.feature.franchise.domain.repository.FranchiseRepository
import com.gokova.myanimelist.feature.franchise.domain.usecase.FranchiseLayoutCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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

@OptIn(ExperimentalCoroutinesApi::class)
class FranchiseViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeFranchiseRepository
    private lateinit var layoutCalculator: FranchiseLayoutCalculator

    private val testNode =
        FranchiseNode(
            animeId = 1L,
            title = "Test Anime",
            titleEnglish = "Test Anime Eng",
            posterMedium = null,
            posterLarge = null,
            mediaType = "tv",
            numEpisodes = 12,
            isInUserList = true,
            userStatus = "watching",
            episodesWatched = 3,
            isRewatching = false,
            isNotInterested = false,
            isOrigin = true,
        )

    private val testGraph =
        FranchiseGraph(
            originAnimeId = 1L,
            nodes = mapOf(1L to testNode),
            edges = emptyList(),
            mainlineSequence = listOf(testNode),
            sideWorksByParent = emptyMap(),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeFranchiseRepository()
        layoutCalculator = FranchiseLayoutCalculator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(animeId: Long = 1L): FranchiseViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("animeId" to animeId))
        return FranchiseViewModel(
            savedStateHandle = savedStateHandle,
            repository = fakeRepository,
            layoutCalculator = layoutCalculator,
        )
    }

    @Test
    fun `initialization observes graph and computes layout`() =
        runTest(testDispatcher) {
            fakeRepository.graphFlow.value = testGraph
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(testGraph, state.graph)
            assertNotNull(state.layout)
            assertEquals(1, state.layout?.nodeLayouts?.size)
        }

    @Test
    fun `onNodeClick and onDismissBottomSheet update selectedNode`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onNodeClick(testNode)
            assertEquals(testNode, viewModel.uiState.value.selectedNode)

            viewModel.onDismissBottomSheet()
            assertNull(viewModel.uiState.value.selectedNode)
        }

    @Test
    fun `onToggleViewMode switches between MAP and LIST`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            assertEquals(FranchiseViewMode.MAP, viewModel.uiState.value.viewMode)

            viewModel.onToggleViewMode()
            assertEquals(FranchiseViewMode.LIST, viewModel.uiState.value.viewMode)

            viewModel.onToggleViewMode()
            assertEquals(FranchiseViewMode.MAP, viewModel.uiState.value.viewMode)
        }

    @Test
    fun `zoom and recenter actions increment triggers`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            assertEquals(0, viewModel.uiState.value.recenterTrigger)
            assertEquals(0, viewModel.uiState.value.fitTrigger)
            assertEquals(0, viewModel.uiState.value.zoomInTrigger)
            assertEquals(0, viewModel.uiState.value.zoomOutTrigger)

            viewModel.onRecenterClick()
            assertEquals(1, viewModel.uiState.value.recenterTrigger)

            viewModel.onFitClick()
            assertEquals(1, viewModel.uiState.value.fitTrigger)

            viewModel.onZoomInClick()
            assertEquals(1, viewModel.uiState.value.zoomInTrigger)

            viewModel.onZoomOutClick()
            assertEquals(1, viewModel.uiState.value.zoomOutTrigger)
        }

    @Test
    fun `onOpenDetails and onBackClick emit ui events`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val events = mutableListOf<FranchiseUiEvent>()
            val job = launch { viewModel.uiEvents.collect { events.add(it) } }

            viewModel.onOpenDetails(10L)
            viewModel.onBackClick()
            advanceUntilIdle()

            assertEquals(2, events.size)
            assertEquals(FranchiseUiEvent.NavigateToDetails(10L), events[0])
            assertEquals(FranchiseUiEvent.NavigateBack, events[1])
            job.cancel()
        }

    @Test
    fun `onIncrementEpisode invokes repository`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onIncrementEpisode(1L)
            advanceUntilIdle()

            assertTrue(fakeRepository.incrementCalledFor.contains(1L))
        }

    @Test
    fun `onIncrementEpisode failure emits ShowSnackbar with error messageRes`() =
        runTest(testDispatcher) {
            fakeRepository.shouldFailIncrement = true
            val viewModel = createViewModel()
            val events = mutableListOf<FranchiseUiEvent>()
            val job = launch { viewModel.uiEvents.collect { events.add(it) } }

            viewModel.onIncrementEpisode(1L)
            advanceUntilIdle()

            assertEquals(1, events.size)
            val snackbar = events[0] as FranchiseUiEvent.ShowSnackbar
            assertEquals(
                com.gokova.myanimelist.feature.franchise.R.string.franchise_error_update_episode,
                snackbar.messageRes,
            )
            assertTrue(snackbar.isError)
            job.cancel()
        }

    private class FakeFranchiseRepository : FranchiseRepository {
        val graphFlow =
            MutableStateFlow(
                FranchiseGraph(1L, emptyMap(), emptyList(), emptyList(), emptyMap()),
            )
        val incrementCalledFor = mutableListOf<Long>()
        val unmarkCalledFor = mutableListOf<Long>()
        var shouldFailIncrement = false

        override fun observeFranchiseGraph(rootAnimeId: Long): Flow<FranchiseGraph> = graphFlow

        override suspend fun incrementWatchedEpisode(animeId: Long): Result<Unit> {
            incrementCalledFor.add(animeId)
            return if (shouldFailIncrement) {
                Result.failure(RuntimeException("Network error"))
            } else {
                Result.success(Unit)
            }
        }

        override suspend fun unmarkNotInterested(animeId: Long): Result<Unit> {
            unmarkCalledFor.add(animeId)
            return Result.success(Unit)
        }
    }
}
