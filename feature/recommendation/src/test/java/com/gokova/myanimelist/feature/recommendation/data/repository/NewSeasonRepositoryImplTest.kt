package com.gokova.myanimelist.feature.recommendation.data.repository

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.WorkInfo
import com.gokova.myanimelist.core.database.dao.NewSeasonDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.NewSeasonAnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.NewSeasonAnimeItem
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.feature.recommendation.data.work.NewSeasonScheduler
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class NewSeasonRepositoryImplTest {
    private lateinit var fakeNewSeasonDao: FakeNewSeasonDao
    private lateinit var fakeUserAnimeListDao: FakeUserAnimeListDao
    private lateinit var fakeScheduler: FakeNewSeasonScheduler
    private lateinit var repository: NewSeasonRepositoryImpl

    private class FakeNewSeasonDao : NewSeasonDao {
        val newSeasonsFlow = MutableStateFlow<List<NewSeasonAnimeItem>>(emptyList())
        val countFlow = MutableStateFlow(0)

        override fun observeNewSeasons(): Flow<List<NewSeasonAnimeItem>> = newSeasonsFlow

        override fun observeNewSeasonCount(): Flow<Int> = countFlow

        override suspend fun getNewSeasonCount(): Int = countFlow.value

        override suspend fun getNewSeasonAnimeIds(parentIds: List<Long>?): List<Long> = emptyList()

        override suspend fun getObsoleteNewSeasonAnimeIds(timestamp: Long): List<Long> = emptyList()

        override suspend fun getExistingAnimeIds(animeIds: List<Long>): List<Long> = emptyList()

        override suspend fun deleteNewSeasons(animeIds: List<Long>): Int = animeIds.size

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) = Unit

        override suspend fun upsertNewSeasons(items: List<NewSeasonAnimeEntity>) = Unit

        override suspend fun deleteOrphanedAnimes(animeIds: List<Long>): Int = 0
    }

    private class FakeUserAnimeListDao : UserAnimeListDao {
        val userAnimeFlow = MutableStateFlow<List<UserAnimeListItem>>(emptyList())

        override fun observeAllUserAnime(): Flow<List<UserAnimeListItem>> = userAnimeFlow

        override suspend fun getAllUserAnime(): List<UserAnimeListItem> = userAnimeFlow.value

        override fun observeUserAnimeByStatus(status: String) = userAnimeFlow

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) = Unit

        override suspend fun upsertUserAnimeList(userAnimeList: List<UserAnimeListEntity>) = Unit

        override suspend fun clearUserAnimeList(): Int = 0
    }

    private class FakeNewSeasonScheduler : NewSeasonScheduler {
        val workInfosFlow = MutableStateFlow<List<WorkInfo>>(emptyList())
        var periodicScheduled = false
        var immediateTriggered = false
        var initialScheduled = false

        override fun schedulePeriodicWork() {
            periodicScheduled = true
        }

        override fun triggerImmediateCalculation() {
            immediateTriggered = true
        }

        override fun scheduleInitialCalculation() {
            initialScheduled = true
        }

        override fun observeFetchWorkInfo(): Flow<List<WorkInfo>> = workInfosFlow
    }

    @Before
    fun setUp() {
        fakeNewSeasonDao = FakeNewSeasonDao()
        fakeUserAnimeListDao = FakeUserAnimeListDao()
        fakeScheduler = FakeNewSeasonScheduler()

        repository =
            NewSeasonRepositoryImpl(
                newSeasonDao = fakeNewSeasonDao,
                userAnimeListDao = fakeUserAnimeListDao,
                scheduler = fakeScheduler,
            )
    }

    private fun createWorkInfo(
        state: WorkInfo.State,
        generation: Int = 0,
    ): WorkInfo =
        WorkInfo(
            id = UUID.randomUUID(),
            state = state,
            tags = emptySet(),
            outputData = Data.EMPTY,
            progress = Data.EMPTY,
            runAttemptCount = 0,
            generation = generation,
            constraints = Constraints.NONE,
            initialDelayMillis = 0L,
            periodicityInfo = null,
            nextScheduleTimeMillis = 0L,
            stopReason = WorkInfo.STOP_REASON_NOT_STOPPED,
        )

    private fun createNewSeasonItem(
        id: Long,
        title: String,
        score: Double = 8.0,
        year: Int = 2026,
        season: String = "spring",
    ) = NewSeasonAnimeItem(
        newSeason =
            NewSeasonAnimeEntity(
                animeId = id,
                parentAnimeId = 999L,
                relationType = "sequel",
                relationTypeFormatted = "Sequel",
            ),
        anime =
            AnimeEntity(
                id = id,
                title = title,
                titleEnglish = title,
                mainPictureMedium = null,
                mainPictureLarge = null,
                mediaType = "tv",
                airingStatus = "finished_airing",
                numEpisodes = 12,
                startSeasonYear = year,
                startSeasonSeason = season,
                meanScore = score,
            ),
        parentAnime =
            AnimeEntity(
                id = 999L,
                title = "Parent Series",
                titleEnglish = null,
                mainPictureMedium = null,
                mainPictureLarge = null,
                mediaType = "tv",
                airingStatus = "finished_airing",
                numEpisodes = 24,
                startSeasonYear = 2024,
                startSeasonSeason = "fall",
                meanScore = 8.5,
            ),
    )

    private fun createUserItem(id: Long) =
        UserAnimeListItem(
            userAnime =
                UserAnimeListEntity(
                    animeId = id,
                    status = "completed",
                    score = 9,
                    numEpisodesWatched = 12,
                    updatedAt = "2026-09-20",
                    isRewatching = false,
                ),
            anime =
                AnimeEntity(
                    id = id,
                    title = "User Anime $id",
                    titleEnglish = null,
                    mainPictureMedium = null,
                    mainPictureLarge = null,
                    mediaType = "tv",
                    airingStatus = "finished_airing",
                    numEpisodes = 12,
                    startSeasonYear = 2026,
                    startSeasonSeason = "winter",
                    meanScore = 8.0,
                ),
        )

    @Test
    fun `observeNewSeasons maps database items and sorts by RELEASE_DATE_DESC`() =
        runTest {
            val item1 = createNewSeasonItem(1L, "Older Season", year = 2025, season = "fall")
            val item2 = createNewSeasonItem(2L, "Newer Season", year = 2026, season = "spring")
            fakeNewSeasonDao.newSeasonsFlow.value = listOf(item1, item2)

            val result =
                repository.observeNewSeasons(NewSeasonSortOption.RELEASE_DATE_DESC).first()

            assertEquals(2, result.size)
            assertEquals(2L, result[0].animeId)
            assertEquals("Newer Season", result[0].displayTitle)
            assertEquals(1L, result[1].animeId)
        }

    @Test
    fun `observeNewSeasons sorts by SCORE_DESC`() =
        runTest {
            val item1 = createNewSeasonItem(1L, "Lower Score", score = 7.5)
            val item2 = createNewSeasonItem(2L, "Higher Score", score = 9.2)
            fakeNewSeasonDao.newSeasonsFlow.value = listOf(item1, item2)

            val result = repository.observeNewSeasons(NewSeasonSortOption.SCORE_DESC).first()

            assertEquals(2, result.size)
            assertEquals(2L, result[0].animeId)
            assertEquals("Higher Score", result[0].displayTitle)
        }

    @Test
    fun `observeNewSeasons sorts by TITLE_ASC`() =
        runTest {
            val item1 = createNewSeasonItem(1L, "Zeta Gundam")
            val item2 = createNewSeasonItem(2L, "Alpha Protocol")
            fakeNewSeasonDao.newSeasonsFlow.value = listOf(item1, item2)

            val result = repository.observeNewSeasons(NewSeasonSortOption.TITLE_ASC).first()

            assertEquals(2, result.size)
            assertEquals(2L, result[0].animeId)
            assertEquals("Alpha Protocol", result[0].displayTitle)
        }

    @Test
    fun `observeNewSeasonState returns EmptyInsufficientData when user has less than 5 anime`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..4L).map { createUserItem(it) }
            fakeNewSeasonDao.countFlow.value = 5

            val state = repository.observeNewSeasonState().first()

            assertEquals(NewSeasonState.EmptyInsufficientData, state)
        }

    @Test
    fun `observeNewSeasonState returns Calculating when worker is running`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeScheduler.workInfosFlow.value = listOf(createWorkInfo(WorkInfo.State.RUNNING))
            fakeNewSeasonDao.countFlow.value = 0

            val state = repository.observeNewSeasonState().first()

            assertEquals(NewSeasonState.Calculating, state)
        }

    @Test
    fun `observeNewSeasonState returns Ready when count is greater than 0`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeNewSeasonDao.countFlow.value = 8

            val state = repository.observeNewSeasonState().first()

            assertTrue(state is NewSeasonState.Ready)
            assertEquals(8, (state as NewSeasonState.Ready).count)
        }

    @Test
    fun `observeNewSeasonState returns Ready when count is greater than 0 while working`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeNewSeasonDao.countFlow.value = 8
            fakeScheduler.workInfosFlow.value = listOf(createWorkInfo(WorkInfo.State.RUNNING))

            val state = repository.observeNewSeasonState().first()

            assertTrue(state is NewSeasonState.Ready)
            assertEquals(8, (state as NewSeasonState.Ready).count)
        }

    @Test
    fun `observeNewSeasonState ignores past cancelled worker if latest worker succeeded`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeNewSeasonDao.countFlow.value = 0
            fakeScheduler.workInfosFlow.value =
                listOf(
                    createWorkInfo(WorkInfo.State.CANCELLED),
                    createWorkInfo(WorkInfo.State.SUCCEEDED),
                )

            val state = repository.observeNewSeasonState().first()

            assertEquals(NewSeasonState.EmptyAllCaughtUp, state)
        }

    @Test
    fun `observeNewSeasonState returns EmptyAllCaughtUp when count is 0 and worker succeeded`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeNewSeasonDao.countFlow.value = 0
            fakeScheduler.workInfosFlow.value = listOf(createWorkInfo(WorkInfo.State.SUCCEEDED))

            val state = repository.observeNewSeasonState().first()

            assertEquals(NewSeasonState.EmptyAllCaughtUp, state)
        }

    @Test
    fun `observeNewSeasonState returns Error when worker failed and count is 0`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeNewSeasonDao.countFlow.value = 0
            fakeScheduler.workInfosFlow.value = listOf(createWorkInfo(WorkInfo.State.FAILED))

            val state = repository.observeNewSeasonState().first()

            assertTrue(state is NewSeasonState.Error)
        }

    @Test
    fun `scheduleInitialOrPeriodicWork triggers initial work when 0 seasons and 5 user anime`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeNewSeasonDao.countFlow.value = 0

            repository.scheduleInitialOrPeriodicWork()

            assertTrue(fakeScheduler.periodicScheduled)
            assertTrue(fakeScheduler.initialScheduled)
        }

    @Test
    fun `scheduleInitialOrPeriodicWork does not trigger initial work when seasons exist`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeNewSeasonDao.countFlow.value = 3

            repository.scheduleInitialOrPeriodicWork()

            assertTrue(fakeScheduler.periodicScheduled)
            assertFalse(fakeScheduler.initialScheduled)
        }

    @Test
    fun `scheduleInitialOrPeriodicWork does not trigger initial work when less than 5 anime`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..3L).map { createUserItem(it) }
            fakeNewSeasonDao.countFlow.value = 0

            repository.scheduleInitialOrPeriodicWork()

            assertTrue(fakeScheduler.periodicScheduled)
            assertFalse(fakeScheduler.initialScheduled)
        }

    @Test
    fun `triggerImmediateEvaluation delegates to scheduler`() =
        runTest {
            repository.triggerImmediateEvaluation()

            assertTrue(fakeScheduler.immediateTriggered)
        }
}
