package com.gokova.myanimelist.feature.recommendation.data.repository

import com.gokova.myanimelist.core.database.dao.RecommendationDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.RecommendationCandidateEntity
import com.gokova.myanimelist.core.database.entity.RecommendationEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.RecommendationItem
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.feature.recommendation.data.work.RecommendationScheduler
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationEngineState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationRepositoryImplTest {
    private lateinit var fakeRecommendationDao: FakeRecommendationDao
    private lateinit var fakeUserAnimeListDao: FakeUserAnimeListDao
    private lateinit var fakeScheduler: FakeRecommendationScheduler
    private lateinit var repository: RecommendationRepositoryImpl

    private class FakeRecommendationDao : RecommendationDao {
        val genreFlow = MutableStateFlow<List<RecommendationItem>>(emptyList())
        val themeFlow = MutableStateFlow<List<RecommendationItem>>(emptyList())
        val countFlow = MutableStateFlow(0)

        override fun observeRecommendationsByGenre(limit: Int) = genreFlow

        override fun observeRecommendationsByTheme(limit: Int) = themeFlow

        override fun observeRecommendationCount(): Flow<Int> = countFlow

        override suspend fun getRecommendationCount(): Int = countFlow.value

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) {
            // no-op
        }

        override suspend fun upsertCandidates(candidates: List<RecommendationCandidateEntity>) {
            // no-op
        }

        override suspend fun getUserAnimeIds(): List<Long> = emptyList()

        override suspend fun getCandidateAnimes(): List<AnimeEntity> = emptyList()

        override suspend fun upsertRecommendations(recommendations: List<RecommendationEntity>) {
            // no-op
        }

        override suspend fun clearRecommendations(): Int = 0

        override suspend fun clearCandidates(): Int = 0

        override suspend fun deleteNonUserData(animeIds: List<Long>): Int = 0
    }

    private class FakeUserAnimeListDao : UserAnimeListDao {
        val userAnimeFlow = MutableStateFlow<List<UserAnimeListItem>>(emptyList())

        override fun observeAllUserAnime(): Flow<List<UserAnimeListItem>> = userAnimeFlow

        override suspend fun getAllUserAnime(): List<UserAnimeListItem> = userAnimeFlow.value

        override fun observeUserAnimeByStatus(status: String) = userAnimeFlow

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) {
            // no-op
        }

        override suspend fun upsertUserAnimeList(userAnimeList: List<UserAnimeListEntity>) {
            // no-op
        }

        override suspend fun clearUserAnimeList(): Int = 0
    }

    private class FakeRecommendationScheduler : RecommendationScheduler {
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

        override fun enqueueEvaluation(isManual: Boolean) = Unit
    }

    @Before
    fun setUp() {
        fakeRecommendationDao = FakeRecommendationDao()
        fakeUserAnimeListDao = FakeUserAnimeListDao()
        fakeScheduler = FakeRecommendationScheduler()

        repository =
            RecommendationRepositoryImpl(
                recommendationDao = fakeRecommendationDao,
                userAnimeListDao = fakeUserAnimeListDao,
                scheduler = fakeScheduler,
            )
    }

    @Suppress("SameParameterValue")
    private fun createRecommendationItem(
        id: Long,
        title: String,
    ) = RecommendationItem(
        recommendation =
            RecommendationEntity(
                animeId = id,
                genreScore = 4.5,
                genreRank = 1,
                themeScore = 4.0,
                themeRank = 2,
                genreMatchPercent = 95,
                themeMatchPercent = 90,
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
                startSeasonYear = 2026,
                startSeasonSeason = "summer",
                meanScore = 8.0,
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
                    updatedAt = "2026-09-19",
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
    fun `observeRecommendations by genre maps database items to domain`() =
        runTest {
            val item = createRecommendationItem(1L, "Top Anime")
            fakeRecommendationDao.genreFlow.value = listOf(item)

            val result = repository.observeRecommendations(RecommendationType.GENRE).first()

            assertEquals(1, result.size)
            assertEquals(1L, result[0].animeId)
            assertEquals("Top Anime", result[0].title)
            assertEquals(95, result[0].genreMatchPercent)
            assertEquals(1, result[0].genreRank)
        }

    @Test
    fun `observeEngineState returns EmptyInsufficientData when user has less than 5 anime`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..3L).map { createUserItem(it) }
            fakeRecommendationDao.countFlow.value = 10

            val state = repository.observeEngineState().first()

            assertEquals(RecommendationEngineState.EmptyInsufficientData, state)
        }

    @Test
    fun `observeEngineState returns Calculating when 5 anime but count is 0`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeRecommendationDao.countFlow.value = 0

            val state = repository.observeEngineState().first()

            assertEquals(RecommendationEngineState.Calculating, state)
        }

    @Test
    fun `observeEngineState returns Ready when recommendations exist and list is ready`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeRecommendationDao.countFlow.value = 50

            val state = repository.observeEngineState().first()

            assertTrue(state is RecommendationEngineState.Ready)
            assertEquals(50, (state as RecommendationEngineState.Ready).totalCount)
        }

    @Test
    fun `scheduleInitialOrPeriodicWork triggers initial work when 0 recs and 5 user anime`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeRecommendationDao.countFlow.value = 0

            repository.scheduleInitialOrPeriodicWork()

            assertTrue(fakeScheduler.periodicScheduled)
            assertTrue(fakeScheduler.initialScheduled)
        }

    @Test
    fun `scheduleInitialOrPeriodicWork does not trigger initial work when recs exist`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..5L).map { createUserItem(it) }
            fakeRecommendationDao.countFlow.value = 50

            repository.scheduleInitialOrPeriodicWork()

            assertTrue(fakeScheduler.periodicScheduled)
            assertFalse(fakeScheduler.initialScheduled)
        }

    @Test
    fun `scheduleInitialOrPeriodicWork does not trigger initial work when less than 5 anime`() =
        runTest {
            fakeUserAnimeListDao.userAnimeFlow.value = (1L..3L).map { createUserItem(it) }
            fakeRecommendationDao.countFlow.value = 0

            repository.scheduleInitialOrPeriodicWork()

            assertTrue(fakeScheduler.periodicScheduled)
            assertFalse(fakeScheduler.initialScheduled)
        }
}
