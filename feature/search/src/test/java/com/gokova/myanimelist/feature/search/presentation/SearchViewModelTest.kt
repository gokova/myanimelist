package com.gokova.myanimelist.feature.search.presentation

import com.gokova.myanimelist.core.ai.model.AiException
import com.gokova.myanimelist.core.datastore.AiPreferences
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.domain.model.AiSearchResult
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem
import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage
import com.gokova.myanimelist.feature.search.domain.repository.SearchRepository
import com.gokova.myanimelist.feature.search.domain.usecase.AiSearchAnimeUseCase
import com.gokova.myanimelist.feature.search.domain.usecase.LoadMoreSearchResultsUseCase
import com.gokova.myanimelist.feature.search.domain.usecase.SearchAnimeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
@Suppress("TooManyFunctions")
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSearchRepository
    private lateinit var fakeAiPreferences: FakeAiPreferences
    private lateinit var searchUseCase: SearchAnimeUseCase
    private lateinit var loadMoreUseCase: LoadMoreSearchResultsUseCase
    private lateinit var aiSearchUseCase: AiSearchAnimeUseCase
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSearchRepository()
        fakeAiPreferences = FakeAiPreferences()
        searchUseCase = SearchAnimeUseCase(fakeRepository)
        loadMoreUseCase = LoadMoreSearchResultsUseCase(fakeRepository)
        aiSearchUseCase = AiSearchAnimeUseCase(fakeRepository)
        viewModel =
            SearchViewModel(
                searchAnimeUseCase = searchUseCase,
                loadMoreSearchResultsUseCase = loadMoreUseCase,
                aiSearchAnimeUseCase = aiSearchUseCase,
                aiPreferences = fakeAiPreferences,
            )
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
            fakeRepository.searchResult = Result.failure(IOException("MAL API 500 error"))

            viewModel.onQueryChange("Fate")
            viewModel.onSearch()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is SearchUiState.Error)
            val error = state as SearchUiState.Error
            assertTrue(error.message?.contains("500") == true)
            assertEquals(R.string.search_error_message, error.messageRes)
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
            assertEquals(R.string.search_load_more_error, state.loadMoreErrorRes)
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

    @Test
    fun toggleModeWithoutApiKeyShowsMissingKeyDialog() =
        runTest(testDispatcher) {
            fakeAiPreferences.setApiKey(null)

            viewModel.onToggleMode()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showMissingKeyDialog)
            assertEquals(SearchMode.STANDARD, viewModel.uiState.value.mode)
        }

    @Test
    fun toggleModeWithApiKeySwitchesMode() =
        runTest(testDispatcher) {
            fakeAiPreferences.setApiKey("test_key")

            viewModel.onToggleMode()
            advanceUntilIdle()

            assertEquals(SearchMode.AI, viewModel.uiState.value.mode)
            assertTrue(viewModel.uiState.value is SearchUiState.AiInitial)
        }

    @Test
    fun onAiSearchWithValidPromptTransitionsToAiContent() =
        runTest(testDispatcher) {
            fakeAiPreferences.setApiKey("valid_key")
            viewModel.onToggleMode()
            advanceUntilIdle()

            val item = createTestItem(1L, "Monster")
            fakeRepository.aiSearchResult =
                Result.success(
                    AiSearchResult(
                        intentSummary = "Psychological thriller anime",
                        explanation = "Top recommendation for suspense",
                        items = listOf(item),
                    ),
                )

            viewModel.onAiPromptChange("thriller anime with mind games")
            viewModel.onAiSearch()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is SearchUiState.AiContent)
            val content = state as SearchUiState.AiContent
            assertEquals(1, content.result.items.size)
            assertEquals("Psychological thriller anime", content.result.intentSummary)
        }

    @Test
    fun onAiSearchWithMissingKeyFailureShowsMissingKeyDialog() =
        runTest(testDispatcher) {
            fakeAiPreferences.setApiKey("key_exists")
            viewModel.onToggleMode()
            advanceUntilIdle()

            fakeRepository.aiSearchResult = Result.failure(AiException.MissingApiKey())
            viewModel.onAiPromptChange("action")
            viewModel.onAiSearch()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showMissingKeyDialog)
        }

    @Test
    fun onAiSearchWithInvalidKeyFailureTransitionsToAiError() =
        runTest(testDispatcher) {
            fakeAiPreferences.setApiKey("invalid_key")
            viewModel.onToggleMode()
            advanceUntilIdle()

            fakeRepository.aiSearchResult =
                Result.failure(AiException.InvalidApiKey("Key rejected"))
            viewModel.onAiPromptChange("action")
            viewModel.onAiSearch()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is SearchUiState.AiError)
            val error = state as SearchUiState.AiError
            assertEquals(R.string.ai_search_error_generic, error.messageRes)
        }

    @Test
    fun onConfigureKeyClickedDismissesDialogAndEmitsNavigateToProfile() =
        runTest(testDispatcher) {
            viewModel.setMissingKeyDialogVisible(true)
            assertTrue(viewModel.uiState.value.showMissingKeyDialog)

            var eventReceived: SearchUiEvent? = null
            val job = launch { eventReceived = viewModel.events.first() }

            viewModel.onConfigureKeyClicked()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showMissingKeyDialog)
            assertTrue(eventReceived is SearchUiEvent.NavigateToProfile)
            job.cancel()
        }

    @Test
    fun modeTogglingPreservesQueryAndPromptDualState() =
        runTest(testDispatcher) {
            fakeAiPreferences.setApiKey("key")
            viewModel.onQueryChange("Gundam")
            assertEquals("Gundam", viewModel.uiState.value.query)

            viewModel.onToggleMode()
            advanceUntilIdle()
            assertEquals(SearchMode.AI, viewModel.uiState.value.mode)

            viewModel.onAiPromptChange("mecha anime recommendations")
            assertEquals("mecha anime recommendations", viewModel.uiState.value.aiPrompt)

            viewModel.onToggleMode()
            advanceUntilIdle()
            assertEquals(SearchMode.STANDARD, viewModel.uiState.value.mode)
            assertEquals("Gundam", viewModel.uiState.value.query)

            viewModel.onToggleMode()
            advanceUntilIdle()
            assertEquals(SearchMode.AI, viewModel.uiState.value.mode)
            assertEquals("mecha anime recommendations", viewModel.uiState.value.aiPrompt)
        }

    @Test
    fun `toggleMode cancels in-flight standard search and preserves AI mode`() =
        runTest {
            fakeRepository.searchLambda = {
                delay(100.milliseconds)
                Result.success(SearchResultPage(items = listOf(createTestItem(1L, "Titan"))))
            }
            viewModel.onQueryChange("Titan")
            viewModel.onSearch()
            assertTrue(viewModel.uiState.value is SearchUiState.Searching)

            viewModel.onToggleMode()
            advanceUntilIdle()

            assertEquals(SearchMode.AI, viewModel.uiState.value.mode)
            assertTrue(viewModel.uiState.value is SearchUiState.AiInitial)
        }

    @Test
    fun onQueryChangeInContentPreservesResultQueryAndResults() =
        runTest(testDispatcher) {
            val item = createTestItem(1L, "Naruto")
            fakeRepository.searchResult = Result.success(SearchResultPage(items = listOf(item)))

            viewModel.onQueryChange("Naruto")
            viewModel.onSearch()
            advanceUntilIdle()

            val initialContent = viewModel.uiState.value as SearchUiState.Content
            assertEquals("Naruto", initialContent.query)
            assertEquals("Naruto", initialContent.resultQuery)
            assertEquals(1, initialContent.animes.size)

            viewModel.onQueryChange("Naruto Shippuden")
            val updatedContent = viewModel.uiState.value as SearchUiState.Content
            assertEquals("Naruto Shippuden", updatedContent.query)
            assertEquals("Naruto", updatedContent.resultQuery)
            assertEquals(1, updatedContent.animes.size)
        }

    @Test
    fun subsequentSearchInContentRetainsPriorResultsWithReplacingStatus() =
        runTest(testDispatcher) {
            val narutoItem = createTestItem(1L, "Naruto")
            val bleachItem = createTestItem(2L, "Bleach")

            fakeRepository.searchResult =
                Result.success(SearchResultPage(items = listOf(narutoItem)))
            viewModel.onQueryChange("Naruto")
            viewModel.onSearch()
            advanceUntilIdle()

            fakeRepository.searchLambda = {
                delay(100.milliseconds)
                Result.success(SearchResultPage(items = listOf(bleachItem)))
            }

            viewModel.onQueryChange("Bleach")
            viewModel.onSearch()

            val replacingState = viewModel.uiState.value as SearchUiState.Content
            assertTrue(replacingState.isReplacing)
            assertEquals("Bleach", replacingState.replacementQuery)
            assertEquals("Naruto", replacingState.resultQuery)
            assertEquals(1, replacingState.animes.size)
            assertEquals("Naruto", replacingState.animes[0].title)

            advanceUntilIdle()

            val finalState = viewModel.uiState.value as SearchUiState.Content
            assertFalse(finalState.isReplacing)
            assertEquals("Bleach", finalState.query)
            assertEquals("Bleach", finalState.resultQuery)
            assertEquals(1, finalState.animes.size)
            assertEquals("Bleach", finalState.animes[0].title)
        }

    @Test
    fun failedReplacementSearchRetainsPriorResultsAndEmitsSnackbar() =
        runTest(testDispatcher) {
            val narutoItem = createTestItem(1L, "Naruto")
            fakeRepository.searchResult =
                Result.success(SearchResultPage(items = listOf(narutoItem)))
            viewModel.onQueryChange("Naruto")
            viewModel.onSearch()
            advanceUntilIdle()

            fakeRepository.searchLambda = {
                delay(50.milliseconds)
                Result.failure(IOException("Server error"))
            }

            var receivedEvent: SearchUiEvent? = null
            val eventJob =
                launch {
                    viewModel.events.collect { receivedEvent = it }
                }

            viewModel.onQueryChange("Bleach")
            viewModel.onSearch()
            assertTrue((viewModel.uiState.value as SearchUiState.Content).isReplacing)

            advanceUntilIdle()

            val content = viewModel.uiState.value as SearchUiState.Content
            assertFalse(content.isReplacing)
            assertEquals("Naruto", content.resultQuery)
            assertEquals(1, content.animes.size)
            assertEquals("Naruto", content.animes[0].title)

            assertTrue(receivedEvent is SearchUiEvent.ShowSnackbar)
            val snackbar = receivedEvent as SearchUiEvent.ShowSnackbar
            assertEquals(R.string.search_replacement_failed, snackbar.messageRes)
            assertEquals("Bleach", snackbar.formatArg)

            eventJob.cancel()
        }

    @Test
    fun onLoadMoreIsSuppressedWhileReplacementSearchIsActive() =
        runTest(testDispatcher) {
            val item1 = createTestItem(1L, "Naruto")
            fakeRepository.searchResult =
                Result.success(SearchResultPage(items = listOf(item1), nextUrl = "page2"))

            viewModel.onQueryChange("Naruto")
            viewModel.onSearch()
            advanceUntilIdle()

            fakeRepository.searchLambda = {
                delay(200.milliseconds)
                Result.success(SearchResultPage(items = listOf(createTestItem(2L, "Bleach"))))
            }

            viewModel.onQueryChange("Bleach")
            viewModel.onSearch()

            var loadMoreCalled = false
            fakeRepository.loadMoreLambda = {
                loadMoreCalled = true
                Result.success(SearchResultPage(items = emptyList()))
            }

            viewModel.onLoadMore()
            assertFalse(loadMoreCalled)

            advanceUntilIdle()
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
        var aiSearchResult: Result<AiSearchResult> =
            Result.success(AiSearchResult("Summary", "Explanation", emptyList()))
        var searchLambda: (suspend (String) -> Result<SearchResultPage>)? = null
        var loadMoreLambda: (suspend (String) -> Result<SearchResultPage>)? = null
        var searchCallCount = 0

        override suspend fun searchAnime(query: String): Result<SearchResultPage> {
            searchCallCount++
            return searchLambda?.invoke(query) ?: searchResult
        }

        override suspend fun loadMore(nextUrl: String): Result<SearchResultPage> =
            loadMoreLambda?.invoke(nextUrl) ?: loadMoreResult

        override suspend fun aiSearchAnime(prompt: String): Result<AiSearchResult> = aiSearchResult
    }

    private class FakeAiPreferences : AiPreferences {
        private val _geminiApiKey = MutableStateFlow<String?>("initial_key")
        override val geminiApiKey: Flow<String?> = _geminiApiKey.asStateFlow()

        fun setApiKey(key: String?) {
            _geminiApiKey.value = key
        }

        override suspend fun setGeminiApiKey(key: String) {
            _geminiApiKey.value = key
        }

        override suspend fun clearGeminiApiKey() {
            _geminiApiKey.value = null
        }
    }
}
