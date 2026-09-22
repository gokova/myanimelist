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
    private lateinit var fakeSyncTracker: FakeNewSeasonSyncTracker

    @Before
    fun setUp() {
        fakeUserAnimeListDao = FakeUserAnimeListDao()
        fakeNewSeasonDao = FakeNewSeasonDao()
        fakeMalApiService = FakeMalApiService()
        fakeSyncTracker = FakeNewSeasonSyncTracker()
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
    fun `doWork stops candidate processing early on timeout and retries parent`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val edge100 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 100L, title = "Candidate 100"),
                    relationType = "side_story",
                    relationTypeFormatted = "Side Story",
                )
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
                AnimeDetailsDto(
                    id = 1L,
                    title = "Anime 1",
                    relatedAnime = listOf(edge100, edge101, edge102),
                )
            fakeMalApiService.detailsMap[100L] =
                AnimeDetailsDto(id = 100L, title = "Candidate 100")

            // Candidate 101 times out, candidate 102 should NOT be processed
            fakeMalApiService.exceptionMap[101L] = SocketTimeoutException("Read timed out")
            fakeMalApiService.detailsMap[102L] =
                AnimeDetailsDto(id = 102L, title = "Candidate 102")

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.retry(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            assertEquals(1, fakeNewSeasonDao.savedNewSeasons.size)
            assertEquals(100L, fakeNewSeasonDao.savedNewSeasons[0].animeId)
            // Early exit on retryable error: candidate 102 was never requested
            assertFalse(fakeMalApiService.requestedAnimeIds.contains(102L))
            assertEquals(-1L, fakeSyncTracker.lastProcessedId)
        }

    @Test
    fun `doWork protects children of failed parent anime and retries`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            // Parent 1 fails with timeout
            fakeMalApiService.exceptionMap[1L] = SocketTimeoutException("Parent 1 timed out")

            fakeNewSeasonDao.existingNewSeasonIds = listOf(101L)
            fakeNewSeasonDao.parentChildMap = mapOf(1L to listOf(101L))

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.retry(), result)
            assertFalse(fakeNewSeasonDao.discardedAnimeIds.contains(101L))
            assertEquals(-1L, fakeSyncTracker.lastProcessedId)
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

            fakeNewSeasonDao.obsoleteAnimeIds = listOf(101L)

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
    fun `doWork fetches details for candidate already in database to find related anime`() =
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

            val edge102 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 102L, title = "Candidate 102"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            fakeMalApiService.detailsMap[101L] =
                AnimeDetailsDto(id = 101L, title = "Candidate 101", relatedAnime = listOf(edge102))
            fakeMalApiService.detailsMap[102L] =
                AnimeDetailsDto(id = 102L, title = "Candidate 102")

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            assertEquals(2, fakeNewSeasonDao.savedNewSeasons.size)
            assertEquals(101L, fakeNewSeasonDao.savedNewSeasons[0].animeId)
            assertEquals(102L, fakeNewSeasonDao.savedNewSeasons[1].animeId)
            assertEquals(101L, fakeNewSeasonDao.savedNewSeasons[1].parentAnimeId)
            // Anime details API was called for candidate 101 to inspect related anime
            assertTrue(fakeMalApiService.requestedAnimeIds.contains(101L))
            assertTrue(fakeMalApiService.requestedAnimeIds.contains(102L))
        }

    @Test
    fun `doWork recursively discovers multi-hop sequels and preserves parent chain`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val edge101 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 101L, title = "Season 2"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            val edge102 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 102L, title = "Season 3"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            val edge103 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 103L, title = "Season 3 Part 2"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )

            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Season 1", relatedAnime = listOf(edge101))
            fakeMalApiService.detailsMap[101L] =
                AnimeDetailsDto(id = 101L, title = "Season 2", relatedAnime = listOf(edge102))
            fakeMalApiService.detailsMap[102L] =
                AnimeDetailsDto(id = 102L, title = "Season 3", relatedAnime = listOf(edge103))
            fakeMalApiService.detailsMap[103L] =
                AnimeDetailsDto(id = 103L, title = "Season 3 Part 2")

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            assertEquals(3, fakeNewSeasonDao.savedNewSeasons.size)

            val s2 = fakeNewSeasonDao.savedNewSeasons.first { it.animeId == 101L }
            val s3 = fakeNewSeasonDao.savedNewSeasons.first { it.animeId == 102L }
            val s3p2 = fakeNewSeasonDao.savedNewSeasons.first { it.animeId == 103L }

            assertEquals(1L, s2.parentAnimeId)
            assertEquals("sequel", s2.relationType)

            assertEquals(101L, s3.parentAnimeId)
            assertEquals("sequel", s3.relationType)

            assertEquals(102L, s3p2.parentAnimeId)
            assertEquals("sequel", s3p2.relationType)
        }

    @Test
    fun `doWork avoids cyclic infinite loops when prequel points back to parent`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val edge101 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 101L, title = "Candidate 101"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            val edgeBackTo1 =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 1L, title = "Anime 1"),
                    relationType = "prequel",
                    relationTypeFormatted = "Prequel",
                )

            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = listOf(edge101))
            fakeMalApiService.detailsMap[101L] =
                AnimeDetailsDto(
                    id = 101L,
                    title = "Candidate 101",
                    relatedAnime = listOf(edgeBackTo1),
                )

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            assertEquals(1, fakeNewSeasonDao.savedNewSeasons.size)
            assertEquals(101L, fakeNewSeasonDao.savedNewSeasons[0].animeId)
            // Anime 1 was only requested once (during user scan), not enqueued again
            assertEquals(1, fakeMalApiService.requestedAnimeIds.count { it == 1L })
        }

    @Test
    fun `doWork limits lateral branching from lateral relations`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val sideStoryEdge =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 101L, title = "Side Story 101"),
                    relationType = "side_story",
                    relationTypeFormatted = "Side Story",
                )
            val altSettingEdge =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 102L, title = "Alt Setting 102"),
                    relationType = "alternative_setting",
                    relationTypeFormatted = "Alternative Setting",
                )

            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = listOf(sideStoryEdge))
            fakeMalApiService.detailsMap[101L] =
                AnimeDetailsDto(
                    id = 101L,
                    title = "Side Story 101",
                    relatedAnime = listOf(altSettingEdge),
                )

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            // Only side story 101 was saved, lateral branch altSetting 102 from 101 was not enqueued
            assertEquals(1, fakeNewSeasonDao.savedNewSeasons.size)
            assertEquals(101L, fakeNewSeasonDao.savedNewSeasons[0].animeId)
            assertFalse(fakeMalApiService.requestedAnimeIds.contains(102L))
        }

    @Test
    fun `doWork continuations discover direct side stories but side stories do not recurse`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val s2Edge =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 101L, title = "Season 2"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            val s2MovieEdge =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 102L, title = "Season 2 Movie"),
                    relationType = "alternative_version",
                    relationTypeFormatted = "Alternative Version",
                )
            val s3Edge =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 103L, title = "Season 3"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            val movieSequelEdge =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 104L, title = "Movie Sequel"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )

            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = listOf(s2Edge))
            fakeMalApiService.detailsMap[101L] =
                AnimeDetailsDto(
                    id = 101L,
                    title = "Season 2",
                    relatedAnime = listOf(s2MovieEdge, s3Edge),
                )
            fakeMalApiService.detailsMap[102L] =
                AnimeDetailsDto(
                    id = 102L,
                    title = "Season 2 Movie",
                    relatedAnime = listOf(movieSequelEdge),
                )
            fakeMalApiService.detailsMap[103L] =
                AnimeDetailsDto(id = 103L, title = "Season 3", relatedAnime = emptyList())

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)

            val savedIds = fakeNewSeasonDao.savedNewSeasons.map { it.animeId }.toSet()
            assertTrue(savedIds.contains(101L))
            assertTrue(savedIds.contains(102L))
            assertTrue(savedIds.contains(103L))
            assertFalse(savedIds.contains(104L))
            assertFalse(fakeMalApiService.requestedAnimeIds.contains(104L))
        }

    @Test
    fun `doWork chunks user anime into batches of 10 and retries for remaining batches`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..15L).map { createUserItem(it) }
            for (id in 1L..15L) {
                val edge =
                    RelatedAnimeEdgeDto(
                        node = AnimeNodeDto(id = id + 1000L, title = "Sequel for $id"),
                        relationType = "sequel",
                        relationTypeFormatted = "Sequel",
                    )
                fakeMalApiService.detailsMap[id] =
                    AnimeDetailsDto(id = id, title = "Anime $id", relatedAnime = listOf(edge))
                fakeMalApiService.detailsMap[id + 1000L] =
                    AnimeDetailsDto(id = id + 1000L, title = "Sequel for $id")
            }

            // Run 1: attempt 0 processes first batch of 10 (ids 1..10)
            val workerRun1 = createWorker(runAttemptCount = 0)
            val result1 = workerRun1.doWork()

            assertEquals(Result.retry(), result1)
            assertEquals(10L, fakeSyncTracker.lastProcessedId)
            assertEquals(10, fakeNewSeasonDao.savedNewSeasons.size)
            for (id in 11L..15L) {
                assertFalse(fakeMalApiService.requestedAnimeIds.contains(id))
            }

            // Run 2: attempt 1 resumes from id 11 to 15
            val workerRun2 = createWorker(runAttemptCount = 1)
            val result2 = workerRun2.doWork()

            assertEquals(Result.success(), result2)
            assertEquals(-1L, fakeSyncTracker.lastProcessedId)
            assertEquals(15, fakeNewSeasonDao.savedNewSeasons.size)
        }

    @Test
    fun `doWork prunes obsolete seasons from previous sync upon completion`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val edge =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 101L, title = "Sequel 101"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = listOf(edge))
            fakeMalApiService.detailsMap[101L] =
                AnimeDetailsDto(id = 101L, title = "Sequel 101")

            fakeNewSeasonDao.obsoleteAnimeIds = listOf(999L)

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.discardedAnimeIds.contains(999L))
            assertFalse(fakeNewSeasonDao.discardedAnimeIds.contains(101L))
            assertTrue(fakeNewSeasonDao.recordedCutoffTimestamp >= 0L)
        }

    @Test
    fun `doWork prunes seasons already present in user list and uses 24 hour window`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val edge =
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 101L, title = "Sequel 101"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                )
            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = listOf(edge))
            fakeMalApiService.detailsMap[101L] =
                AnimeDetailsDto(id = 101L, title = "Sequel 101")

            fakeNewSeasonDao.existingNewSeasonIds = listOf(3L, 101L)
            fakeNewSeasonDao.obsoleteAnimeIds = listOf(999L)

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.discardedAnimeIds.contains(999L))
            assertTrue(fakeNewSeasonDao.discardedAnimeIds.contains(3L))
            assertFalse(fakeNewSeasonDao.discardedAnimeIds.contains(101L))
            val now = System.currentTimeMillis()
            val window = FetchNewSeasonsWorker.PRUNE_OBSOLETE_WINDOW_MS
            val expectedMinCutoff = now - window - 5000L
            val expectedMaxCutoff = now - window + 5000L
            val cutoff = fakeNewSeasonDao.recordedCutoffTimestamp
            assertTrue(cutoff in expectedMinCutoff..expectedMaxCutoff)
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

    @Test
    fun `doWork scopes tracker state between manual and periodic runs`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..15L).map { createUserItem(it) }
            val manualWorker = createWorker(isManual = true)
            manualWorker.doWork()

            assertEquals(
                10L,
                fakeSyncTracker.getLastProcessedUserAnimeId(NewSeasonSyncTracker.SCOPE_MANUAL),
            )
            assertEquals(
                -1L,
                fakeSyncTracker.getLastProcessedUserAnimeId(NewSeasonSyncTracker.SCOPE_PERIODIC),
            )
        }

    @Test
    fun `doWork bounds root candidate traversal at MAX_CANDIDATES_PER_ROOT`() =
        runTest {
            fakeUserAnimeListDao.userAnimeList = (1L..5L).map { createUserItem(it) }
            val edges =
                (101L..155L).map { id ->
                    RelatedAnimeEdgeDto(
                        node = AnimeNodeDto(id = id, title = "Sequel $id"),
                        relationType = "sequel",
                        relationTypeFormatted = "Sequel",
                    )
                }
            fakeMalApiService.detailsMap[1L] =
                AnimeDetailsDto(id = 1L, title = "Anime 1", relatedAnime = edges)

            val worker = createWorker()
            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertTrue(fakeNewSeasonDao.transactionCalled)
            assertEquals(50, fakeNewSeasonDao.savedNewSeasons.size)
        }

    private fun createWorker(
        runAttemptCount: Int = 0,
        isManual: Boolean = false,
    ): FetchNewSeasonsWorker {
        val fakeContext =
            object : ContextWrapper(null) {
                override fun getApplicationContext(): Context = this
            }
        val inputData =
            androidx.work.Data
                .Builder()
                .putBoolean(NewSeasonScheduler.KEY_IS_MANUAL, isManual)
                .build()
        val builder =
            TestListenableWorkerBuilder<FetchNewSeasonsWorker>(fakeContext)
                .setInputData(inputData)
                .setRunAttemptCount(runAttemptCount)
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
                                fakeSyncTracker,
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

    private class FakeNewSeasonSyncTracker : NewSeasonSyncTracker {
        val lastProcessedMap = mutableMapOf<String, Long>()
        val recordedSyncStartTimeMap = mutableMapOf<String, Long>()

        var lastProcessedId: Long
            get() =
                lastProcessedMap[NewSeasonSyncTracker.SCOPE_PERIODIC]
                    ?: lastProcessedMap[NewSeasonSyncTracker.DEFAULT_SCOPE]
                    ?: -1L
            set(value) {
                lastProcessedMap[NewSeasonSyncTracker.SCOPE_PERIODIC] = value
                lastProcessedMap[NewSeasonSyncTracker.DEFAULT_SCOPE] = value
            }

        override fun getLastProcessedUserAnimeId(scopeKey: String): Long =
            lastProcessedMap[scopeKey] ?: -1L

        override fun setLastProcessedUserAnimeId(
            id: Long,
            scopeKey: String,
        ) {
            lastProcessedMap[scopeKey] = id
        }

        override fun getSyncStartTime(scopeKey: String): Long =
            recordedSyncStartTimeMap[scopeKey] ?: 0L

        override fun setSyncStartTime(
            timestamp: Long,
            scopeKey: String,
        ) {
            recordedSyncStartTimeMap[scopeKey] = timestamp
        }

        override fun reset(scopeKey: String) {
            lastProcessedMap[scopeKey] = -1L
            recordedSyncStartTimeMap[scopeKey] = 0L
        }
    }

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
        var recordedCutoffTimestamp: Long = -1L
        var obsoleteAnimeIds: List<Long> = emptyList()
        val allSavedNewSeasons = mutableListOf<NewSeasonAnimeEntity>()
        val allDiscardedAnimeIds = mutableListOf<Long>()
        var transactionCalled: Boolean = false

        override suspend fun getNewSeasonAnimeIds(parentIds: List<Long>?): List<Long> {
            if (parentIds == null) return existingNewSeasonIds
            return parentIds.flatMap { parentChildMap[it] ?: emptyList() }
        }

        override suspend fun getObsoleteNewSeasonAnimeIds(timestamp: Long): List<Long> {
            recordedCutoffTimestamp = timestamp
            return obsoleteAnimeIds
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
