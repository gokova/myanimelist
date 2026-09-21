package com.gokova.myanimelist.feature.recommendation.data.work

import android.content.Context
import android.content.ContextWrapper
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.gokova.myanimelist.core.database.dao.NewSeasonDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.NewSeasonAnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.NewSeasonAnimeItem
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeDetailsDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.model.RelatedAnimeEdgeDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException

class FetchNewSeasonsWorkerTest {
    private lateinit var fakeUserAnimeListDao: FakeUserAnimeListDao
    private lateinit var fakeNewSeasonDao: FakeNewSeasonDao
    private lateinit var fakeMalApiService: FakeMalApiService

    @Before
    fun setUp() {
        fakeUserAnimeListDao = FakeUserAnimeListDao()
        fakeNewSeasonDao = FakeNewSeasonDao()
        fakeMalApiService = FakeMalApiService()
    }

    @Test
    fun `doWork returns success and skips when user anime list is less than threshold`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..3L).map { createUserItem(it) }
            val worker = createWorker()

            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            assertEquals(0, fakeNewSeasonDao.savedNewSeasons.size)
        }

    @Test
    fun `doWork succeeds and saves new seasons when requests succeed`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val sequelNode = AnimeNodeDto(id = 101L, title = "Sequel 101")
            val edge =
                RelatedAnimeEdgeDto(
                    node = sequelNode,
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = listOf(edge))
            fakeMalApiService.detailsMap[101L] =
                AnimeDetailsDto(id = 101L, title = "Sequel 101")

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            assertEquals(1, fakeNewSeasonDao.savedNewSeasons.size)
            assertEquals(101L, fakeNewSeasonDao.savedNewSeasons[0].animeId)
        }

    @Test
    fun `doWork continues batch when one candidate details times out and saves rest`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val edge101 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 101L, title = "Candidate 101"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            val edge102 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 102L, title = "Candidate 102"),
                    relationType = "side_story",
                    relationTypeFormatted = "Side Story",
                )
            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = listOf(edge101, edge102))

            // Candidate 101 times out, candidate 102 succeeds
            fakeMalApiService.exceptionMap[101L] = SocketTimeoutException("Read timed out")
            fakeMalApiService.detailsMap[102L] =
                AnimeDetailsDto(id = 102L, title = "Candidate 102")

            fakeNewSeasonDao.existingNewSeasonIds = listOf(101L, 999L)

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.retry(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            assertEquals(1, fakeNewSeasonDao.savedNewSeasons.size)
            assertEquals(102L, fakeNewSeasonDao.savedNewSeasons[0].animeId)
            // 101 failed to refresh, so it is preserved (not in discarded)
            assertFalse(fakeNewSeasonDao.discardedAnimeIds.contains(101L))
            // 999 is no longer a candidate, so it is discarded
            assertTrue(fakeNewSeasonDao.discardedAnimeIds.contains(999L))
        }

    @Test
    fun `doWork protects children of failed parent anime and retries`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            // Parent 1 fails with timeout
            fakeMalApiService.exceptionMap[1L] = SocketTimeoutException("Parent 1 timed out")

            // Parent 2 succeeds with candidate 201
            val edge201 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 201L, title = "Candidate 201"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            fakeMalApiService.detailsMap[2L] =
                AnimeDetailsDto(id = 2L, title = "Anime 2", relatedAnime = listOf(edge201))
            fakeMalApiService.detailsMap[201L] =
                AnimeDetailsDto(id = 201L, title = "Candidate 201")

            // Database currently has child 101 belonging to parent 1, and 999 which is obsolete
            fakeNewSeasonDao.existingNewSeasonIds = listOf(101L, 999L)
            fakeNewSeasonDao.parentChildMap = mapOf(1L to listOf(101L))

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.retry(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            // 201 was fetched and saved
            assertTrue(fakeNewSeasonDao.savedNewSeasons.any { it.animeId == 201L })
            // 101 belongs to failed parent 1, so it must be protected (not discarded)
            assertFalse(fakeNewSeasonDao.discardedAnimeIds.contains(101L))
            // 999 is obsolete, so it is discarded
            assertTrue(fakeNewSeasonDao.discardedAnimeIds.contains(999L))
        }

    @Test
    fun `doWork prunes 404 candidate cleanly and returns success without retry`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val edge101 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 101L, title = "Candidate 101"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = listOf(edge101))

            // Candidate 101 returns HTTP 404 Not Found
            val errorBody = "".toResponseBody(null)
            fakeMalApiService.exceptionMap[101L] =
                HttpException(Response.error<Any>(404, errorBody))

            fakeNewSeasonDao.existingNewSeasonIds = listOf(101L)

            val worker = createWorker()
            val result = worker.doWork()

            // 404 is permanent, not retryable, so worker returns success
            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            // 101 is discarded from database
            assertTrue(fakeNewSeasonDao.discardedAnimeIds.contains(101L))
            // Nothing was saved
            assertEquals(0, fakeNewSeasonDao.savedNewSeasons.size)
        }

    @Test
    fun `doWork skips network call for candidate already in database`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val edge101 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 101L, title = "Candidate 101"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = listOf(edge101))

            // Candidate 101 already exists in Room database
            fakeNewSeasonDao.existingAnimeIdsInDb = setOf(101L)

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            assertEquals(1, fakeNewSeasonDao.savedNewSeasons.size)
            assertEquals(101L, fakeNewSeasonDao.savedNewSeasons[0].animeId)
            // Anime details API was NOT called for candidate 101
            assertFalse(fakeMalApiService.requestedAnimeIds.contains(101L))
        }

    @Test
    fun `doWork caps uncached candidate fetches per run at 50 and reschedules retry`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val edges =
                (101L..152L).map { id ->
                    RelatedAnimeEdgeDto(
                        node = AnimeNodeDto(id = id, title = "Candidate $id"),
                        relationType = "sequel",
                        relationTypeFormatted = "Sequel",
                    )
                }
            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = edges)

            for (id in 101L..152L) {
                fakeMalApiService.detailsMap[id] =
                    AnimeDetailsDto(id = id, title = "Candidate $id")
            }

            fakeNewSeasonDao.existingNewSeasonIds = listOf(152L)

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.retry(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            assertEquals(50, fakeNewSeasonDao.savedNewSeasons.size)
            assertFalse(fakeNewSeasonDao.discardedAnimeIds.contains(152L))
            val candidateApiFetches =
                fakeMalApiService.requestedAnimeIds.filter { it in 101L..152L }
            assertEquals(50, candidateApiFetches.size)
        }

    @Test
    fun `doWork retries and preserves database when all candidates fail`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val edge =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 101L, title = "Candidate 101"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = listOf(edge))
            fakeMalApiService.exceptionMap[101L] = SocketTimeoutException("timeout")

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.retry(), result)
            assertFalse(fakeNewSeasonDao.transactionCalled)
        }

    @Test
    fun `doWork retries and preserves database when all user anime requests fail`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            for (id in 1L..5L) {
                fakeMalApiService.exceptionMap[id] = SocketTimeoutException("timeout")
            }

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.retry(), result)
            assertFalse(fakeNewSeasonDao.transactionCalled)
        }

    private fun createWorker(): FetchNewSeasonsWorker {
        val fakeContext =
            object : ContextWrapper(null) {
                override fun getApplicationContext(): Context = this
            }
        val builder =
            TestListenableWorkerBuilder<FetchNewSeasonsWorker>(fakeContext)
                .setWorkerFactory(
                    object : WorkerFactory() {
                        override fun createWorker(
                            appContext: Context,
                            workerClassName: String,
                            workerParameters: WorkerParameters,
                        ): ListenableWorker =
                            FetchNewSeasonsWorker(
                                appContext,
                                workerParameters,
                                fakeMalApiService,
                                fakeUserAnimeListDao,
                                fakeNewSeasonDao,
                            )
                    },
                )
        return builder.build()
    }

    private fun createUserItem(id: Long) =
        UserAnimeListItem(
            userAnime =
                UserAnimeListEntity(
                    animeId = id,
                    status = "completed",
                    score = 9,
                    numEpisodesWatched = 12,
                    updatedAt = "2026-03-24T12:00:00Z",
                    isRewatching = false,
                ),
            anime =
                AnimeEntity(
                    id = id,
                    title = "User Anime $id",
                    titleEnglish = "User Anime $id",
                    mainPictureMedium = null,
                    mainPictureLarge = null,
                    mediaType = "tv",
                    airingStatus = "finished_airing",
                    numEpisodes = 12,
                    startSeasonYear = 2024,
                    startSeasonSeason = "fall",
                    meanScore = 8.0,
                ),
        )

    private class FakeUserAnimeListDao : UserAnimeListDao {
        var userAnimeList: List<UserAnimeListItem> = emptyList()

        override suspend fun getAllUserAnime(): List<UserAnimeListItem> = userAnimeList

        override fun observeAllUserAnime(): Flow<List<UserAnimeListItem>> = emptyFlow()

        override fun observeUserAnimeByStatus(status: String): Flow<List<UserAnimeListItem>> =
            emptyFlow()

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) = Unit

        override suspend fun upsertUserAnimeList(userAnimeList: List<UserAnimeListEntity>) = Unit

        override suspend fun clearUserAnimeList(): Int = 0
    }

    private class FakeNewSeasonDao : NewSeasonDao {
        var existingNewSeasonIds: List<Long> = emptyList()
        var existingAnimeIdsInDb: Set<Long> = emptySet()
        var parentChildMap: Map<Long, List<Long>> = emptyMap()
        val allSavedNewSeasons = mutableListOf<NewSeasonAnimeEntity>()
        val allDiscardedAnimeIds = mutableListOf<Long>()
        var transactionCalled: Boolean = false

        override suspend fun getNewSeasonAnimeIds(parentIds: List<Long>?): List<Long> {
            if (parentIds == null) return existingNewSeasonIds
            return parentIds.flatMap { parentChildMap[it] ?: emptyList() }
        }

        override suspend fun getExistingAnimeIds(animeIds: List<Long>): List<Long> =
            animeIds.filter { it in existingAnimeIdsInDb }

        override suspend fun deleteNewSeasons(animeIds: List<Long>): Int = animeIds.size

        override suspend fun saveNewSeasonsTransaction(
            animes: List<AnimeEntity>,
            newSeasons: List<NewSeasonAnimeEntity>,
            discardedAnimeIds: List<Long>,
        ) {
            transactionCalled = true
            allSavedNewSeasons.addAll(newSeasons)
            allDiscardedAnimeIds.addAll(discardedAnimeIds)
        }

        val savedNewSeasons: List<NewSeasonAnimeEntity> get() = allSavedNewSeasons
        val discardedAnimeIds: List<Long> get() = allDiscardedAnimeIds

        override fun observeNewSeasons(): Flow<List<NewSeasonAnimeItem>> = emptyFlow()

        override fun observeNewSeasonCount(): Flow<Int> = emptyFlow()

        override suspend fun getNewSeasonCount(): Int = existingNewSeasonIds.size

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) = Unit

        override suspend fun upsertNewSeasons(items: List<NewSeasonAnimeEntity>) = Unit

        override suspend fun deleteOrphanedAnimes(animeIds: List<Long>): Int = 0
    }

    private class FakeMalApiService : MalApiService {
        val detailsMap = mutableMapOf<Long, AnimeDetailsDto>()
        val exceptionMap = mutableMapOf<Long, Exception>()
        val requestedAnimeIds = mutableListOf<Long>()

        override suspend fun getAnimeDetails(
            animeId: Long,
            fields: String,
        ): AnimeDetailsDto {
            requestedAnimeIds.add(animeId)
            exceptionMap[animeId]?.let { throw it }
            return detailsMap[animeId] ?: AnimeDetailsDto(id = animeId, title = "Title $animeId")
        }

        override suspend fun getUserAnimeList(
            fields: String,
            limit: Int,
            offset: Int,
            nsfw: Boolean,
        ) = throw UnsupportedOperationException()

        override suspend fun getUserAnimeListNextPage(url: String) =
            throw UnsupportedOperationException()

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
    }
}
