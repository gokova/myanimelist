package com.gokova.myanimelist.feature.taste.presentation

import com.gokova.myanimelist.feature.taste.domain.model.TasteType
import com.gokova.myanimelist.feature.taste.domain.model.UserAnimeRecord
import com.gokova.myanimelist.feature.taste.domain.repository.TasteRepository
import com.gokova.myanimelist.feature.taste.domain.usecase.ObserveTasteAnalyticsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasteViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class FakeRepository : TasteRepository {
        override fun observeUserAnimeRecords(): Flow<List<UserAnimeRecord>> =
            flowOf(
                listOf(
                    UserAnimeRecord(1, "A1", null, "completed", 9, 12, 12, listOf("Action")),
                    UserAnimeRecord(2, "A2", null, "completed", 8, 12, 12, listOf("Action")),
                    UserAnimeRecord(3, "A3", null, "completed", 7, 12, 12, listOf("Action")),
                ),
            )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyLoadsAnalytics() =
        runTest {
            val useCase = ObserveTasteAnalyticsUseCase(FakeRepository())
            val viewModel = TasteViewModel(useCase)

            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }

            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(1, state.genres.size)
            assertEquals("Action", state.genres[0].name)
            assertEquals(TasteType.GENRE, state.selectedType)

            collectJob.cancel()
        }

    @Test
    fun onTypeSelected_changesTypeAndIncrementsRecenter() =
        runTest {
            val useCase = ObserveTasteAnalyticsUseCase(FakeRepository())
            val viewModel = TasteViewModel(useCase)

            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            testScheduler.advanceUntilIdle()

            viewModel.onTypeSelected(TasteType.THEME)
            testScheduler.advanceUntilIdle()

            assertEquals(TasteType.THEME, viewModel.uiState.value.selectedType)
            assertEquals(1, viewModel.uiState.value.recenterTrigger)

            collectJob.cancel()
        }

    @Test
    fun onBubbleSelected_and_dismissBottomSheet_updatesState() =
        runTest {
            val useCase = ObserveTasteAnalyticsUseCase(FakeRepository())
            val viewModel = TasteViewModel(useCase)

            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            testScheduler.advanceUntilIdle()

            val bubble =
                viewModel.uiState.value.genres
                    .first()
            viewModel.onBubbleSelected(bubble)
            testScheduler.advanceUntilIdle()

            val selected = viewModel.uiState.value.selectedBubble
            assertNotNull(selected)
            assertEquals("Action", selected?.name)

            viewModel.onDismissBottomSheet()
            testScheduler.advanceUntilIdle()

            assertNull(viewModel.uiState.value.selectedBubble)

            collectJob.cancel()
        }
}
