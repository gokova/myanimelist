package com.gokova.myanimelist.feature.details.data.repository

import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeDetailsDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.model.AnimeRecommendationEdgeDto
import com.gokova.myanimelist.core.network.model.MyListStatusDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.sql.SQLException

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeDetailsRepositoryImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeUserAnimeListDao
    private lateinit var fakeApi: FakeMalApiService
    private lateinit var repository: AnimeDetailsRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeUserAnimeListDao()
        fakeApi = FakeMalApiService()
        repository =
            AnimeDetailsRepositoryImpl(
                malApiService = fakeApi,
                userAnimeListDao = fakeDao,
                ioDispatcher = testDispatcher,
            )
    }

    @Test
    fun `addAnimeToPlanToWatch delegates to atomic transaction and updates cache`() =
        runTest(testDispatcher) {
            val animeId = 874L
            fakeApi.updateStatusResult =
                MyListStatusDto(
                    status = "plan_to_watch",
                    score = 0,
                    numEpisodesWatched = 0,
                    updatedAt = "2026-09-29T10:00:00Z",
                )

            val result = repository.addAnimeToPlanToWatch(animeId)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(1, fakeDao.cleanedUserAnimes.size)
            assertEquals(animeId, fakeDao.cleanedUserAnimes.first().animeId)
            assertEquals("plan_to_watch", fakeDao.cleanedUserAnimes.first().status)
        }

    @Test
    fun `addAnimeToPlanToWatch failure in atomic transaction rolls back and returns failure`() =
        runTest(testDispatcher) {
            val animeId = 874L
            fakeApi.updateStatusResult =
                MyListStatusDto(
                    status = "plan_to_watch",
                    score = 0,
                    numEpisodesWatched = 0,
                )
            fakeDao.transactionException = SQLException("Disk I/O error or constraint violation")

            val result = repository.addAnimeToPlanToWatch(animeId)
            advanceUntilIdle()

            assertTrue(result.isFailure)
            assertTrue(fakeDao.cleanedUserAnimes.isEmpty())
        }

    @Test
    fun `fetchFreshAnimeDetails for untracked anime does not persist to Room`() =
        runTest(testDispatcher) {
            val untrackedId = 9999L
            fakeApi.detailsMap[untrackedId] =
                AnimeDetailsDto(id = untrackedId, title = "Untracked Anime")

            val result = repository.fetchFreshAnimeDetails(untrackedId)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals("Untracked Anime", result.getOrNull()?.displayTitle)
            // Room should NOT contain this anime entity
            assertEquals(null, fakeDao.getAnimeEntityById(untrackedId))
        }

    @Test
    fun `addAnimeToPlanToWatch persists cached entity if not previously in Room`() =
        runTest(testDispatcher) {
            val untrackedId = 9999L
            fakeApi.detailsMap[untrackedId] =
                AnimeDetailsDto(id = untrackedId, title = "Untracked Anime")
            repository.fetchFreshAnimeDetails(untrackedId)
            advanceUntilIdle()

            // Confirm not in Room before add
            assertEquals(null, fakeDao.getAnimeEntityById(untrackedId))

            fakeApi.updateStatusResult =
                MyListStatusDto(
                    status = "plan_to_watch",
                    score = 0,
                    numEpisodesWatched = 0,
                    updatedAt = "2026-10-06T10:00:00Z",
                )

            val addResult = repository.addAnimeToPlanToWatch(untrackedId)
            advanceUntilIdle()

            assertTrue(addResult.isSuccess)
            // Anime entity must now be persisted to Room to satisfy foreign key
            val savedEntity = fakeDao.getAnimeEntityById(untrackedId)
            assertTrue(savedEntity != null)
            assertEquals("Untracked Anime", savedEntity?.title)
        }

    @Test
    fun `freshDetailsCache evicts oldest entries when exceeding max capacity`() =
        runTest(testDispatcher) {
            // Fetch 12 anime details (MAX_CACHE_SIZE is 10)
            (1L..12L).forEach { id ->
                fakeApi.detailsMap[id] =
                    AnimeDetailsDto(
                        id = id,
                        title = "Anime $id",
                        recommendations =
                            listOf(
                                AnimeRecommendationEdgeDto(
                                    node = AnimeNodeDto(id = id + 1000L, title = "Rec $id"),
                                    numRecommendations = 5,
                                ),
                            ),
                    )
                repository.fetchFreshAnimeDetails(id)
            }
            advanceUntilIdle()

            // Anime 12 should be in cache with rich recommendations
            val details12 = repository.observeAnimeDetails(12L).first()
            assertEquals(1, details12?.recommendations?.size)

            // Anime 1 should have been evicted from freshDetailsCache; Room has no entity so returns null
            val details1 = repository.observeAnimeDetails(1L).first()
            assertFalse(details1?.recommendations?.isNotEmpty() == true)
        }

    private class FakeUserAnimeListDao : UserAnimeListDao {
        val animeEntities = mutableMapOf<Long, MutableStateFlow<AnimeEntity?>>()
        val userAnimeItems = mutableMapOf<Long, MutableStateFlow<UserAnimeListItem?>>()
        val cleanedUserAnimes = mutableListOf<UserAnimeListEntity>()
        var transactionException: Exception? = null

        override fun observeAllUserAnime(): Flow<List<UserAnimeListItem>> = emptyFlow()

        override suspend fun getAllUserAnime(): List<UserAnimeListItem> = emptyList()

        override fun observeUserAnimeByStatus(status: String) = emptyFlow<List<UserAnimeListItem>>()

        override fun observeUserAnimeById(animeId: Long): Flow<UserAnimeListItem?> =
            userAnimeItems.getOrPut(animeId) { MutableStateFlow(null) }

        override suspend fun getUserAnimeById(animeId: Long) = userAnimeItems[animeId]?.value

        override fun observeAnimeEntityById(animeId: Long): Flow<AnimeEntity?> =
            animeEntities.getOrPut(animeId) { MutableStateFlow(null) }

        override suspend fun getAnimeEntityById(animeId: Long): AnimeEntity? =
            animeEntities[animeId]?.value

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) {
            animes.forEach { anime ->
                animeEntities.getOrPut(anime.id) { MutableStateFlow(null) }.value = anime
            }
        }

        private fun dummyAnime(id: Long) =
            AnimeEntity(
                id = id,
                title = "Anime $id",
                titleEnglish = null,
                mainPictureMedium = null,
                mainPictureLarge = null,
                mediaType = null,
                airingStatus = null,
                numEpisodes = null,
                startSeasonYear = null,
                startSeasonSeason = null,
                meanScore = null,
            )

        override suspend fun upsertUserAnimeList(userAnimeList: List<UserAnimeListEntity>) {
            userAnimeList.forEach { userAnime ->
                val animeEntity =
                    animeEntities[userAnime.animeId]?.value ?: dummyAnime(userAnime.animeId)
                userAnimeItems
                    .getOrPut(userAnime.animeId) {
                        MutableStateFlow(null)
                    }.value = UserAnimeListItem(userAnime = userAnime, anime = animeEntity)
            }
        }

        override suspend fun clearUserAnimeList(): Int = 0

        override suspend fun deleteRecommendation(animeId: Long): Int = 1

        override suspend fun deleteNewSeasonAnime(animeId: Long): Int = 1

        override suspend fun addAnimeToUserListAndClean(userAnime: UserAnimeListEntity) {
            transactionException?.let { throw it }
            cleanedUserAnimes.add(userAnime)
            upsertUserAnimeList(listOf(userAnime))
        }
    }

    private class FakeMalApiService : MalApiService {
        val detailsMap = mutableMapOf<Long, AnimeDetailsDto>()
        var updateStatusResult: MyListStatusDto = MyListStatusDto(status = "plan_to_watch")

        override suspend fun getAnimeDetails(
            animeId: Long,
            fields: String,
        ) = detailsMap[animeId] ?: AnimeDetailsDto(id = animeId, title = "Anime $animeId")

        override suspend fun getUserAnimeList(
            fields: String,
            limit: Int,
            offset: Int,
            nsfw: Boolean,
        ) = throw UnsupportedOperationException()

        override suspend fun getAnimeListNextPage(url: String) =
            throw UnsupportedOperationException()

        override suspend fun searchAnime(
            query: String,
            limit: Int,
            offset: Int,
            fields: String,
            nsfw: Boolean,
        ) = throw UnsupportedOperationException()

        override suspend fun getAnimeRanking(
            rankingType: String,
            limit: Int,
            offset: Int,
            fields: String,
            nsfw: Boolean,
        ) = throw UnsupportedOperationException()

        override suspend fun getSeasonalAnime(
            year: Int,
            season: String,
            limit: Int,
            offset: Int,
            fields: String,
        ) = throw UnsupportedOperationException()

        override suspend fun updateMyListStatus(
            animeId: Long,
            status: String,
            numWatchedEpisodes: Int,
            score: Int,
        ): MyListStatusDto = updateStatusResult

        override suspend fun getUserProfile(
            userId: String,
            fields: String,
        ) = throw UnsupportedOperationException()
    }
}
