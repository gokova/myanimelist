package com.gokova.myanimelist.feature.mylist.data.repository

import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeDetailsDto
import com.gokova.myanimelist.core.network.model.MyListStatusDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class UserAnimeListStatusRepositoryImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeMalApiService: FakeMalApiService
    private lateinit var fakeUserAnimeListDao: FakeUserAnimeListDao
    private lateinit var repository: UserAnimeListStatusRepositoryImpl

    @Before
    fun setUp() {
        fakeMalApiService = FakeMalApiService()
        fakeUserAnimeListDao = FakeUserAnimeListDao()
        repository =
            UserAnimeListStatusRepositoryImpl(
                malApiService = fakeMalApiService,
                userAnimeListDao = fakeUserAnimeListDao,
                ioDispatcher = testDispatcher,
            )
    }

    @Test
    fun `updateAnimeStatus updates list in MAL API and stores in Room`() =
        runTest(testDispatcher) {
            fakeMalApiService.updateStatusResponse =
                MyListStatusDto(
                    status = "watching",
                    score = 9,
                    numEpisodesWatched = 4,
                    updatedAt = "2026-10-19T10:00:00Z",
                    isRewatching = true,
                )

            val params =
                UpdateUserAnimeStatusParams(
                    status = "watching",
                    score = 9,
                    numWatchedEpisodes = 4,
                    isRewatching = true,
                )
            val result = repository.updateAnimeStatus(animeId = 100L, params = params)

            assertTrue(result.isSuccess)
            val update = result.getOrThrow()
            assertEquals(100L, update.animeId)
            assertEquals("watching", update.status)
            assertEquals(9, update.score)
            assertEquals(4, update.numEpisodesWatched)
            assertTrue(update.isRewatching)

            assertEquals(100L, fakeMalApiService.lastUpdatedAnimeId)
            assertEquals("watching", fakeMalApiService.lastStatus)
            assertEquals(9, fakeMalApiService.lastScore)
            assertEquals(4, fakeMalApiService.lastEpisodes)
            assertEquals(true, fakeMalApiService.lastRewatching)

            val saved = fakeUserAnimeListDao.savedUserAnime
            assertEquals(100L, saved?.animeId)
            assertEquals("watching", saved?.status)
            assertEquals(9, saved?.score)
            assertEquals(4, saved?.numEpisodesWatched)
            assertTrue(saved?.isRewatching == true)
        }

    @Test
    fun `updateAnimeStatus with score 0 transmits 0 to reset score`() =
        runTest(testDispatcher) {
            fakeMalApiService.updateStatusResponse =
                MyListStatusDto(
                    status = "plan_to_watch",
                    score = 0,
                    numEpisodesWatched = 0,
                    updatedAt = "2026-10-19T10:00:00Z",
                    isRewatching = false,
                )

            val params =
                UpdateUserAnimeStatusParams(
                    status = "plan_to_watch",
                    score = 0,
                )
            val result = repository.updateAnimeStatus(animeId = 200L, params = params)

            assertTrue(result.isSuccess)
            assertEquals(0, fakeMalApiService.lastScore)
            assertEquals(0, fakeUserAnimeListDao.savedUserAnime?.score)
        }

    @Test
    fun `updateAnimeStatus returns failure when network fails`() =
        runTest(testDispatcher) {
            fakeMalApiService.networkException = IOException("Network connection failed")

            val params = UpdateUserAnimeStatusParams(status = "completed")
            val result = repository.updateAnimeStatus(animeId = 100L, params = params)

            assertTrue(result.isFailure)
            assertEquals(null, fakeUserAnimeListDao.savedUserAnime)
        }

    @Test
    fun `updateAnimeStatus aborts and returns failure if missing entity fetch fails`() =
        runTest(testDispatcher) {
            fakeUserAnimeListDao.returnNullEntity = true
            fakeMalApiService.detailsException = IOException("Failed to fetch missing entity")

            val params = UpdateUserAnimeStatusParams(status = "watching")
            val result = repository.updateAnimeStatus(animeId = 555L, params = params)

            assertTrue(result.isFailure)
            assertEquals(null, fakeMalApiService.lastUpdatedAnimeId)
            assertEquals(null, fakeUserAnimeListDao.savedUserAnime)
        }

    @Test
    fun `deleteAnimeFromList successfully calls MAL DELETE and deletes local entry`() =
        runTest(testDispatcher) {
            fakeMalApiService.deleteResponse = Response.success(Unit)

            val result = repository.deleteAnimeFromList(animeId = 100L)

            assertTrue(result.isSuccess)
            assertEquals(100L, fakeMalApiService.lastDeletedAnimeId)
            assertEquals(100L, fakeUserAnimeListDao.lastDeletedAnimeId)
        }

    @Test
    fun `deleteAnimeFromList treats 404 NOT FOUND as success and removes local entry`() =
        runTest(testDispatcher) {
            fakeMalApiService.deleteResponse =
                Response.error(404, "".toResponseBody(null))

            val result = repository.deleteAnimeFromList(animeId = 100L)

            assertTrue(result.isSuccess)
            assertEquals(100L, fakeUserAnimeListDao.lastDeletedAnimeId)
        }

    @Test
    fun `deleteAnimeFromList returns failure on 500 server error and keeps local entry`() =
        runTest(testDispatcher) {
            fakeMalApiService.deleteResponse =
                Response.error(500, "Internal Server Error".toResponseBody(null))

            val result = repository.deleteAnimeFromList(animeId = 100L)

            assertTrue(result.isFailure)
            assertEquals(null, fakeUserAnimeListDao.lastDeletedAnimeId)
        }

    private class FakeMalApiService : MalApiService {
        var updateStatusResponse: MyListStatusDto = MyListStatusDto()
        var deleteResponse: Response<Unit> = Response.success(Unit)
        var networkException: Throwable? = null
        var detailsException: Throwable? = null

        var lastUpdatedAnimeId: Long? = null
        var lastStatus: String? = null
        var lastRewatching: Boolean? = null
        var lastScore: Int? = null
        var lastEpisodes: Int? = null
        var lastDeletedAnimeId: Long? = null

        override suspend fun updateMyListStatus(
            animeId: Long,
            status: String?,
            isRewatching: Boolean?,
            score: Int?,
            numWatchedEpisodes: Int?,
        ): MyListStatusDto {
            networkException?.let { throw it }
            lastUpdatedAnimeId = animeId
            lastStatus = status
            lastRewatching = isRewatching
            lastScore = score
            lastEpisodes = numWatchedEpisodes
            return updateStatusResponse
        }

        override suspend fun deleteMyListStatus(animeId: Long): Response<Unit> {
            networkException?.let { throw it }
            lastDeletedAnimeId = animeId
            return deleteResponse
        }

        override suspend fun getUserProfile(
            userId: String,
            fields: String,
        ) = throw UnsupportedOperationException()

        override suspend fun getUserAnimeList(
            fields: String,
            limit: Int,
            offset: Int,
            nsfw: Boolean,
        ) = throw UnsupportedOperationException()

        override suspend fun getAnimeListNextPage(url: String) =
            throw UnsupportedOperationException()

        override suspend fun getAnimeDetails(
            animeId: Long,
            fields: String,
        ): AnimeDetailsDto {
            detailsException?.let { throw it }
            return AnimeDetailsDto(id = animeId, title = "Test Anime")
        }

        override suspend fun getSeasonalAnime(
            year: Int,
            season: String,
            limit: Int,
            offset: Int,
            fields: String,
        ) = throw UnsupportedOperationException()

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
    }

    private class FakeUserAnimeListDao : UserAnimeListDao {
        var savedUserAnime: UserAnimeListEntity? = null
        var lastDeletedAnimeId: Long? = null
        var returnNullEntity: Boolean = false

        override suspend fun addAnimeToUserListAndClean(userAnime: UserAnimeListEntity) {
            savedUserAnime = userAnime
        }

        override suspend fun deleteUserAnime(animeId: Long): Int {
            lastDeletedAnimeId = animeId
            return 1
        }

        override fun observeAllUserAnime(): Flow<List<UserAnimeListItem>> = flowOf(emptyList())

        override suspend fun getAllUserAnime(): List<UserAnimeListItem> = emptyList()

        override fun observeUserAnimeByStatus(status: String): Flow<List<UserAnimeListItem>> =
            flowOf(emptyList())

        override fun observeUserAnimeById(animeId: Long): Flow<UserAnimeListItem?> = flowOf(null)

        override suspend fun getUserAnimeById(animeId: Long): UserAnimeListItem? = null

        override fun observeAnimeEntityById(animeId: Long): Flow<AnimeEntity?> = flowOf(null)

        override suspend fun getAnimeEntityById(animeId: Long): AnimeEntity? =
            if (returnNullEntity) {
                null
            } else {
                AnimeEntity(
                    id = animeId,
                    title = "Test Anime",
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
            }

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) = Unit

        override suspend fun upsertUserAnimeList(userAnimeList: List<UserAnimeListEntity>) = Unit

        override suspend fun clearUserAnimeList(): Int = 0

        override suspend fun deleteRecommendation(animeId: Long): Int = 0

        override suspend fun deleteNewSeasonAnime(animeId: Long): Int = 0

        override suspend fun deleteIgnoredAnime(animeId: Long): Int = 0
    }
}
