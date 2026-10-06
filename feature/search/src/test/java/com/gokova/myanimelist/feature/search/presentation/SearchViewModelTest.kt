package com.gokova.myanimelist.feature.search.presentation

import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem
import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage
import com.gokova.myanimelist.feature.search.domain.repository.SearchRepository
import com.gokova.myanimelist.feature.search.domain.usecase.LoadMoreSearchResultsUseCase
import com.gokova.myanimelist.feature.search.domain.usecase.SearchAnimeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSearchRepository
    private lateinit var searchUseCase: SearchAnimeUseCase
    private lateinit var loadMoreUseCase: LoadMoreSearchResultsUseCase
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSearchRepository()
        searchUseCase = SearchAnimeUseCase(fakeRepository)
        loadMoreUseCase = LoadMoreSearchResultsUseCase(fakeRepository)
        viewModel = SearchViewModel(searchUseCase, loadMoreUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsInitialWithEmptyQuery() {
        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Initial)
        assertEquals("", state.query)
    }

    @Test
    fun onQueryChangeUpdatesQueryInState() {
        viewModel.onQueryChange("attack")
        val state = viewModel.uiState.value
        assertEquals("attack", state.query)
        assertTrue(state is SearchUiState.Initial)
    }

    @Test
    fun onSearchWithShortQueryDoesNotTriggerSearch() =
        runTest(testDispatcher) {
            viewModel.onQueryChange("ab")
            viewModel.onSearch()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is SearchUiState.Initial)
            assertEquals(0, fakeRepository.searchCallCount)
        }

    @Test
    fun onSearchWithValidQueryTransitionsToSearchingThenContent() =
        runTest(testDispatcher) {
            val testItem = createTestItem(1L, "Demon Slayer")
            fakeRepository.searchResult =
                Result.success(SearchResultPage(items = listOf(testItem), nextUrl = "next_url"))

            viewModel.onQueryChange("Demon Slayer")
            viewModel.onSearch()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is SearchUiState.Content)
            val content = state as SearchUiState.Content
            assertEquals(1, content.animes.size)
            assertEquals("Demon Slayer", content.animes[0].title)
            assertTrue(content.hasMore)
            assertFalse(content.isLoadingMore)
        }

    @Test
    fun onSearchWithEmptyResultsTransitionsToEmptyState() =
        runTest(testDispatcher) {
            fakeRepository.searchResult = Result.success(SearchResultPage(items = emptyList()))

            viewModel.onQueryChange("NonExistentAnime123")
            viewModel.onSearch()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is SearchUiState.Empty)
        }

    @Test
    fun onSearchFailureTransitionsToErrorState() =
        runTest(testDispatcher) {
            fakeRepository.searchResult = Result.failure(IOException(" MAL API 500 error"))

            viewModel.onQueryChange("Fate")
            viewModel.onSearch()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is SearchUiState.Error)
            val error = state as SearchUiState.Error
            assertTrue(error.message.contains("500"))
        }

    @Test
    fun onLoadMoreAppendsItemsAndUpdatesNextUrl() =
        runTest(testDispatcher) {
            val item1 = createTestItem(1L, "Season 1")
            val item2 = createTestItem(2L, "Season 2")
            fakeRepository.searchResult =
                Result.success(SearchResultPage(items = listOf(item1), nextUrl = "https://page2"))
            fakeRepository.loadMoreResult =
                Result.success(SearchResultPage(items = listOf(item2), nextUrl = null))

            viewModel.onQueryChange("Season")
            viewModel.onSearch()
            advanceUntilIdle()

            viewModel.onLoadMore()
            advanceUntilIdle()

            val state = viewModel.uiState.value as SearchUiState.Content
            assertEquals(2, state.animes.size)
            assertFalse(state.hasMore)
            assertFalse(state.isLoadingMore)
        }

    @Test
    fun onLoadMoreFailureEmitsSnackbarAndPreservesContent() =
        runTest(testDispatcher) {
            val item1 = createTestItem(1L, "Anime 1")
            fakeRepository.searchResult =
                Result.success(SearchResultPage(items = listOf(item1), nextUrl = "https://page2"))
            fakeRepository.loadMoreResult = Result.failure(IOException("Network disconnected"))

            viewModel.onQueryChange("Anime")
            viewModel.onSearch()
            advanceUntilIdle()

            var receivedEvent: SearchUiEvent? = null
            val eventJob =
                launch {
                    receivedEvent = viewModel.events.first()
                }

            viewModel.onLoadMore()
            advanceUntilIdle()

            val state = viewModel.uiState.value as SearchUiState.Content
            assertEquals(1, state.animes.size)
            assertFalse(state.isLoadingMore)
            assertEquals("Network disconnected", state.loadMoreError)
            assertTrue(receivedEvent is SearchUiEvent.ShowSnackbar)
            assertEquals(
                R.string.search_load_more_error,
                (receivedEvent as SearchUiEvent.ShowSnackbar).messageRes,
            )
            eventJob.cancel()
        }

    @Test
    fun onQueryChangeWhileSearchingCancelsSearchAndDoesNotReplaceNewQuery() =
        runTest(testDispatcher) {
            fakeRepository.searchLambda = {
                delay(1_000.milliseconds)
                Result.success(SearchResultPage(items = listOf(createTestItem(1L, "Digimon"))))
            }

            viewModel.onQueryChange("Digimon")
            viewModel.onSearch()
            assertTrue(viewModel.uiState.value is SearchUiState.Searching)

            viewModel.onQueryChange("Pokemon")
            val state = viewModel.uiState.value
            assertTrue(state is SearchUiState.Initial)
            assertEquals("Pokemon", state.query)

            advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertTrue(finalState is SearchUiState.Initial)
            assertEquals("Pokemon", finalState.query)
        }

    @Test
    fun onLoadMoreDeduplicatesItemsWithSameId() =
        runTest(testDispatcher) {
            val item1 = createTestItem(1L, "Digimon Adventure")
            val item2 = createTestItem(2L, "Digimon Tamers")
            val duplicateItem1 = createTestItem(1L, "Digimon Adventure Duplicate")
            val item3 = createTestItem(3L, "Digimon Frontier")

            fakeRepository.searchResult =
                Result.success(SearchResultPage(items = listOf(item1, item2), nextUrl = "page2"))
            fakeRepository.loadMoreResult =
                Result.success(
                    SearchResultPage(items = listOf(duplicateItem1, item3), nextUrl = null),
                )

            viewModel.onQueryChange("Digimon")
            viewModel.onSearch()
            advanceUntilIdle()

            viewModel.onLoadMore()
            advanceUntilIdle()

            val state = viewModel.uiState.value as SearchUiState.Content
            assertEquals(3, state.animes.size)
            assertEquals(listOf(1L, 2L, 3L), state.animes.map { it.id })
        }

    @Test
    fun onLoadMoreIgnoresResultsIfQueryChangedWhileLoading() =
        runTest(testDispatcher) {
            val item1 = createTestItem(1L, "Digimon")
            val item2 = createTestItem(2L, "Digimon 02")

            fakeRepository.searchResult =
                Result.success(SearchResultPage(items = listOf(item1), nextUrl = "page2"))
            fakeRepository.loadMoreLambda = {
                delay(1_000.milliseconds)
                Result.success(SearchResultPage(items = listOf(item2), nextUrl = null))
            }

            viewModel.onQueryChange("Digimon")
            viewModel.onSearch()
            advanceUntilIdle()

            viewModel.onLoadMore()
            val loadingState = viewModel.uiState.value as SearchUiState.Content
            assertTrue(loadingState.isLoadingMore)

            viewModel.onQueryChange("Pokemon")
            val canceledState = viewModel.uiState.value as SearchUiState.Content
            assertEquals("Pokemon", canceledState.query)
            assertFalse(canceledState.isLoadingMore)

            advanceUntilIdle()

            val finalState = viewModel.uiState.value as SearchUiState.Content
            assertEquals("Pokemon", finalState.query)
            assertEquals(1, finalState.animes.size)
            assertEquals(1L, finalState.animes[0].id)
            assertFalse(finalState.isLoadingMore)
        }

    @Test
    fun onClearQueryResetsToInitialState() =
        runTest(testDispatcher) {
            val item1 = createTestItem(1L, "Anime 1")
            fakeRepository.searchResult = Result.success(SearchResultPage(items = listOf(item1)))

            viewModel.onQueryChange("Anime")
            viewModel.onSearch()
            advanceUntilIdle()

            viewModel.onClearQuery()

            val state = viewModel.uiState.value
            assertTrue(state is SearchUiState.Initial)
            assertEquals("", state.query)
        }

    @Test
    fun onRetrySearchReTriggersSearch() =
        runTest(testDispatcher) {
            fakeRepository.searchResult = Result.failure(IOException("Failed first attempt"))
            viewModel.onQueryChange("Gintama")
            viewModel.onSearch()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is SearchUiState.Error)

            fakeRepository.searchResult =
                Result.success(SearchResultPage(items = listOf(createTestItem(1L, "Gintama"))))
            viewModel.onRetrySearch()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is SearchUiState.Content)
        }

    private fun createTestItem(
        id: Long,
        title: String,
    ): SearchAnimeItem =
        SearchAnimeItem(
            id = id,
            title = title,
            englishTitle = title,
            displayTitle = title,
            subtitleTitle = null,
            thumbnailUrl = null,
            mediaType = "tv",
            numEpisodes = 12,
            meanScore = 8.5,
            genres = listOf("Action"),
            userStatus = null,
            tasteMatchPercent = 90,
        )

    private class FakeSearchRepository : SearchRepository {
        var searchResult: Result<SearchResultPage> = Result.success(SearchResultPage())
        var loadMoreResult: Result<SearchResultPage> = Result.success(SearchResultPage())
        var searchLambda: (suspend (String) -> Result<SearchResultPage>)? = null
        var loadMoreLambda: (suspend (String) -> Result<SearchResultPage>)? = null
        var searchCallCount = 0

        override suspend fun searchAnime(query: String): Result<SearchResultPage> {
            searchCallCount++
            return searchLambda?.invoke(query) ?: searchResult
        }

        override suspend fun loadMore(nextUrl: String): Result<SearchResultPage> =
            loadMoreLambda?.invoke(nextUrl) ?: loadMoreResult
    }
}
