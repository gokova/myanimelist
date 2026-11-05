package com.gokova.myanimelist.feature.search.data.repository

import com.gokova.myanimelist.core.ai.client.AiSearchClient
import com.gokova.myanimelist.core.ai.model.AiCandidateHint
import com.gokova.myanimelist.core.ai.model.AiException
import com.gokova.myanimelist.core.ai.model.AiSearchInterpretation
import com.gokova.myanimelist.core.database.dao.IgnoredAnimeDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.GenreEntity
import com.gokova.myanimelist.core.database.entity.IgnoredAnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.datastore.AiPreferences
import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.model.GenreDto
import com.gokova.myanimelist.core.network.model.PagingDto
import com.gokova.myanimelist.feature.search.data.local.SearchLocalDataSource
import com.gokova.myanimelist.feature.search.data.mapper.AiCandidateResolver
import com.gokova.myanimelist.feature.search.data.remote.SearchRemoteDataSource
import com.gokova.myanimelist.feature.search.domain.algorithm.SearchTasteMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@Suppress("TooManyFunctions")
class SearchRepositoryImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRemoteDataSource: FakeSearchRemoteDataSource
    private lateinit var fakeUserDao: FakeUserAnimeListDao
    private lateinit var fakeIgnoredDao: FakeIgnoredAnimeDao
    private lateinit var localDataSource: SearchLocalDataSource
    private lateinit var fakeAiPreferences: FakeAiPreferences
    private lateinit var fakeAiSearchClient: FakeAiSearchClient
    private lateinit var candidateResolver: AiCandidateResolver
    private lateinit var repository: SearchRepositoryImpl

    @Before
    fun setUp() {
        fakeRemoteDataSource = FakeSearchRemoteDataSource()
        fakeUserDao = FakeUserAnimeListDao()
        fakeIgnoredDao = FakeIgnoredAnimeDao()
        localDataSource = SearchLocalDataSource(fakeUserDao, fakeIgnoredDao)
        fakeAiPreferences = FakeAiPreferences()
        fakeAiSearchClient = FakeAiSearchClient()
        candidateResolver = AiCandidateResolver(SearchTasteMatcher())
        repository =
            SearchRepositoryImpl(
                remoteDataSource = fakeRemoteDataSource,
                localDataSource = localDataSource,
                candidateResolver = candidateResolver,
                aiPreferences = fakeAiPreferences,
                aiSearchClient = fakeAiSearchClient,
                ioDispatcher = testDispatcher,
            )
    }

    @Test
    fun searchAnimeSuccessfulMapsItemsAndTasteMatchAndStatus() =
        runTest(testDispatcher) {
            fakeUserDao.userAnimeList = createTestUserList()
            fakeRemoteDataSource.searchResponse = createTestSearchResponse()

            val result = repository.searchAnime("Action")

            assertTrue(result.isSuccess)
            val page = result.getOrThrow()
            assertEquals(2, page.items.size)
            assertEquals("https://api.myanimelist.net/v2/anime?offset=20", page.nextUrl)

            val firstItem = page.items[0]
            assertEquals(1L, firstItem.id)
            assertEquals("watching", firstItem.userStatus)
            assertNotNull(firstItem.tasteMatchPercent)

            val secondItem = page.items[1]
            assertEquals(999L, secondItem.id)
            assertNull(secondItem.userStatus)
            assertNotNull(secondItem.tasteMatchPercent)
        }

    @Test
    fun searchAnimeDeduplicatesDuplicateNodesInResponse() =
        runTest(testDispatcher) {
            val duplicateResponse =
                AnimeListResponseDto(
                    data =
                        listOf(
                            AnimeListEntryDto(node = AnimeNodeDto(id = 3033L, title = "Digimon")),
                            AnimeListEntryDto(
                                node = AnimeNodeDto(id = 3033L, title = "Digimon Dup"),
                            ),
                            AnimeListEntryDto(
                                node = AnimeNodeDto(id = 3034L, title = "Digimon 02"),
                            ),
                        ),
                )
            fakeRemoteDataSource.searchResponse = duplicateResponse

            val result = repository.searchAnime("Digimon")

            assertTrue(result.isSuccess)
            val page = result.getOrThrow()
            assertEquals(2, page.items.size)
            assertEquals(listOf(3033L, 3034L), page.items.map { it.id })
        }

    @Test
    fun searchAnimeFailureReturnsFailureResult() =
        runTest(testDispatcher) {
            fakeRemoteDataSource.errorToThrow = IOException("Connection reset")

            val result = repository.searchAnime("Action")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IOException)
        }

    @Test
    fun loadMoreSuccessfulReturnsNextPage() =
        runTest(testDispatcher) {
            val response =
                AnimeListResponseDto(
                    data =
                        listOf(
                            AnimeListEntryDto(
                                node = AnimeNodeDto(id = 200L, title = "Next Page Anime"),
                            ),
                        ),
                    paging = null,
                )
            fakeRemoteDataSource.nextPageResponse = response

            val result = repository.loadMore("https://api.myanimelist.net/v2/anime?offset=20")

            assertTrue(result.isSuccess)
            val page = result.getOrThrow()
            assertEquals(1, page.items.size)
            assertNull(page.nextUrl)
        }

    @Test
    fun loadMoreFailureReturnsFailureResult() =
        runTest(testDispatcher) {
            fakeRemoteDataSource.errorToThrow = IOException("Timeout")

            val result = repository.loadMore("https://api.myanimelist.net/v2/anime?offset=20")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IOException)
        }

    @Test
    fun aiSearchAnimeWhenApiKeyMissingReturnsMissingApiKeyFailure() =
        runTest(testDispatcher) {
            fakeAiPreferences.setApiKey(null)

            val result = repository.aiSearchAnime("dark fantasy")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AiException.MissingApiKey)
        }

    @Test
    fun aiSearchAnimeWhenSuccessfulResolvesCandidates() =
        runTest(testDispatcher) {
            fakeAiPreferences.setApiKey("valid_key")
            fakeAiSearchClient.searchResult =
                Result.success(
                    AiSearchInterpretation(
                        intentSummary = "Dark fantasy recommendations",
                        explanation = "Top picks matching your criteria",
                        candidates =
                            listOf(
                                AiCandidateHint(
                                    title = "Anime 1",
                                    reason = "Gritty world and characters",
                                ),
                            ),
                    ),
                )
            fakeRemoteDataSource.searchResponse = createTestSearchResponse()
            fakeUserDao.userAnimeList = createTestUserList()

            val result = repository.aiSearchAnime("dark fantasy")

            assertTrue(result.isSuccess)
            val searchResult = result.getOrThrow()
            assertEquals("Dark fantasy recommendations", searchResult.intentSummary)
            assertEquals(1, searchResult.items.size)
            assertEquals("Anime 1", searchResult.items[0].title)
            assertEquals("Gritty world and characters", searchResult.items[0].aiReason)
        }

    private fun createTestUserList(): List<UserAnimeListItem> =
        (1L..5L).map { id ->
            UserAnimeListItem(
                userAnime =
                    UserAnimeListEntity(
                        animeId = id,
                        status = if (id == 1L) "watching" else "completed",
                        score = 9,
                        numEpisodesWatched = 12,
                        updatedAt = "2026-01-01",
                        isRewatching = false,
                    ),
                anime =
                    AnimeEntity(
                        id = id,
                        title = "Anime $id",
                        titleEnglish = "Anime $id",
                        mainPictureMedium = null,
                        mainPictureLarge = null,
                        mediaType = "tv",
                        airingStatus = "finished_airing",
                        numEpisodes = 12,
                        startSeasonYear = 2024,
                        startSeasonSeason = "spring",
                        meanScore = 8.0,
                        genres = listOf(GenreEntity(1, "Action")),
                    ),
            )
        }

    private fun createTestSearchResponse(): AnimeListResponseDto =
        AnimeListResponseDto(
            data =
                listOf(
                    AnimeListEntryDto(
                        node =
                            AnimeNodeDto(
                                id = 1L,
                                title = "Anime 1",
                                genres = listOf(GenreDto(1, "Action")),
                                mean = 8.5,
                                numListUsers = 50000,
                            ),
                    ),
                    AnimeListEntryDto(
                        node =
                            AnimeNodeDto(
                                id = 999L,
                                title = "Untracked Anime",
                                genres = listOf(GenreDto(1, "Action")),
                                mean = 8.0,
                                numListUsers = 20000,
                            ),
                    ),
                ),
            paging = PagingDto(next = "https://api.myanimelist.net/v2/anime?offset=20"),
        )

    private class FakeSearchRemoteDataSource : SearchRemoteDataSource {
        var searchResponse = AnimeListResponseDto()
        var nextPageResponse = AnimeListResponseDto()
        var errorToThrow: Throwable? = null

        override suspend fun searchAnime(query: String): AnimeListResponseDto {
            errorToThrow?.let { throw it }
            return searchResponse
        }

        override suspend fun fetchNextPage(nextUrl: String): AnimeListResponseDto {
            errorToThrow?.let { throw it }
            return nextPageResponse
        }
    }

    private class FakeUserAnimeListDao : UserAnimeListDao {
        var userAnimeList: List<UserAnimeListItem> = emptyList()

        override suspend fun getAllUserAnime(): List<UserAnimeListItem> = userAnimeList

        override fun observeAllUserAnime(): Flow<List<UserAnimeListItem>> = flowOf(userAnimeList)

        override fun observeUserAnimeByStatus(status: String): Flow<List<UserAnimeListItem>> =
            flowOf(userAnimeList.filter { it.userAnime.status == status })

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) {
            // No-op for test
        }

        override suspend fun upsertUserAnimeList(userAnimeList: List<UserAnimeListEntity>) {
            // No-op for test
        }

        override suspend fun clearUserAnimeList(): Int = 0

        override fun observeUserAnimeById(animeId: Long): Flow<UserAnimeListItem?> =
            flowOf(userAnimeList.firstOrNull { it.userAnime.animeId == animeId })

        override suspend fun getUserAnimeById(animeId: Long): UserAnimeListItem? =
            userAnimeList.firstOrNull { it.userAnime.animeId == animeId }

        override fun observeAnimeEntityById(animeId: Long): Flow<AnimeEntity?> =
            flowOf(userAnimeList.firstOrNull { it.anime.id == animeId }?.anime)

        override suspend fun getAnimeEntityById(animeId: Long): AnimeEntity? =
            userAnimeList.firstOrNull { it.anime.id == animeId }?.anime

        override suspend fun deleteRecommendation(animeId: Long): Int = 0

        override suspend fun deleteNewSeasonAnime(animeId: Long): Int = 0

        override suspend fun deleteIgnoredAnime(animeId: Long): Int = 0

        override suspend fun addAnimeToUserListAndClean(userAnime: UserAnimeListEntity) {
            // No-op for test
        }

        override suspend fun deleteUserAnime(animeId: Long): Int = 0
    }

    private class FakeIgnoredAnimeDao : IgnoredAnimeDao {
        var ignoredList: List<Long> = emptyList()

        override fun observeIgnoredAnimeIds(): Flow<List<Long>> = flowOf(ignoredList)

        override suspend fun getIgnoredAnimeIds(): List<Long> = ignoredList

        override fun observeIsIgnored(animeId: Long): Flow<Boolean> = flowOf(animeId in ignoredList)

        override suspend fun isIgnored(animeId: Long): Boolean = animeId in ignoredList

        override suspend fun upsertIgnoredAnime(entity: IgnoredAnimeEntity) {
            // No-op for test
        }

        override suspend fun upsertIgnoredAnimes(entities: List<IgnoredAnimeEntity>) {
            // No-op for test
        }

        override fun observeIgnoredAnimeCount(): Flow<Int> = flowOf(ignoredList.size)

        override suspend fun deleteIgnoredAnime(animeId: Long): Int = 0

        override fun observeAllIgnoredAnime(): Flow<List<IgnoredAnimeEntity>> = flowOf(emptyList())

        override suspend fun getAllIgnoredAnime(): List<IgnoredAnimeEntity> = emptyList()
    }

    private class FakeAiPreferences : AiPreferences {
        private val _geminiApiKey = MutableStateFlow<String?>(null)
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

    private class FakeAiSearchClient : AiSearchClient {
        var searchResult: Result<AiSearchInterpretation> =
            Result.failure(UnsupportedOperationException())
        var testResult: Result<Unit> = Result.success(Unit)

        override suspend fun searchCandidates(
            prompt: String,
            apiKey: String,
        ): Result<AiSearchInterpretation> = searchResult

        override suspend fun validateApiKey(apiKey: String): Result<Unit> = testResult
    }
}
