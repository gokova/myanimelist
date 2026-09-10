package com.gokova.myanimelist.feature.mylist.data.repository

import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.datastore.SyncPreferences
import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.model.MyListStatusDto
import com.gokova.myanimelist.feature.mylist.data.remote.AnimeListRemoteDataSource
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MyListRepositoryImplTest {
    private lateinit var fakeDao: FakeUserAnimeListDao
    private lateinit var fakeRemoteDataSource: FakeAnimeListRemoteDataSource
    private lateinit var fakeSyncPreferences: FakeSyncPreferences
    private lateinit var repository: MyListRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeUserAnimeListDao()
        fakeRemoteDataSource = FakeAnimeListRemoteDataSource()
        fakeSyncPreferences = FakeSyncPreferences()
        repository =
            MyListRepositoryImpl(
                dao = fakeDao,
                remoteDataSource = fakeRemoteDataSource,
                syncPreferences = fakeSyncPreferences,
            )
    }

    @Test
    fun `syncUserAnimeList skips sync when inside cooldown and not forced`() =
        runTest {
            fakeSyncPreferences.timestamp = System.currentTimeMillis()

            val statuses = repository.syncUserAnimeList(force = false).toList()

            assertEquals(listOf(SyncStatus.SkippedCooldown), statuses)
            assertEquals(0, fakeRemoteDataSource.fetchCallCount)
        }

    @Test
    fun `syncUserAnimeList executes when cooldown has expired`() =
        runTest {
            val oldTime = System.currentTimeMillis() - COOLDOWN_EXPIRED_OFFSET
            fakeSyncPreferences.timestamp = oldTime

            fakeRemoteDataSource.entries =
                listOf(
                    createEntryDto(id = 1L, title = "Frieren", score = 10),
                )

            val statuses = repository.syncUserAnimeList(force = false).toList()

            assertEquals(listOf(SyncStatus.Started, SyncStatus.Completed), statuses)
            assertEquals(1, fakeRemoteDataSource.fetchCallCount)
            assertEquals(1, fakeDao.syncedAnimes.size)
            assertEquals(1, fakeDao.syncedUserList.size)
            assertTrue(fakeSyncPreferences.timestamp > oldTime)
        }

    @Test
    fun `syncUserAnimeList executes when force is true even inside cooldown`() =
        runTest {
            fakeSyncPreferences.timestamp = System.currentTimeMillis()

            val statuses = repository.syncUserAnimeList(force = true).toList()

            assertEquals(listOf(SyncStatus.Started, SyncStatus.Completed), statuses)
            assertEquals(1, fakeRemoteDataSource.fetchCallCount)
        }

    @Test
    fun `syncUserAnimeList rethrows CancellationException and does not swallow it`() =
        runTest {
            fakeSyncPreferences.timestamp = 0L
            fakeRemoteDataSource.exceptionToThrow = CancellationException("Job cancelled")

            try {
                repository.syncUserAnimeList(force = true).toList()
                fail("Expected CancellationException to be thrown")
            } catch (e: CancellationException) {
                assertEquals("Job cancelled", e.message)
            }
        }

    @Test
    fun `syncUserAnimeList returns failure on network error`() =
        runTest {
            fakeSyncPreferences.timestamp = 0L
            fakeRemoteDataSource.exceptionToThrow = IOException("Connection reset")

            val statuses = repository.syncUserAnimeList(force = true).toList()

            assertEquals(2, statuses.size)
            assertEquals(SyncStatus.Started, statuses[0])
            assertTrue(statuses[1] is SyncStatus.Failure)
            assertEquals("Connection reset", (statuses[1] as SyncStatus.Failure).error.message)
        }

    @Test
    fun `observeUserAnimeList delegates to dao correctly`() =
        runTest {
            val anime =
                AnimeEntity(
                    id = 1L,
                    title = "Steins;Gate",
                    titleEnglish = "Steins;Gate",
                    mainPictureMedium = null,
                    mainPictureLarge = null,
                    mediaType = "TV",
                    airingStatus = "finished_airing",
                    numEpisodes = 24,
                    startSeasonYear = 2011,
                    startSeasonSeason = "spring",
                    meanScore = 9.1,
                )
            val userAnime =
                UserAnimeListEntity(
                    animeId = 1L,
                    status = "completed",
                    score = 10,
                    numEpisodesWatched = 24,
                    updatedAt = "2023-01-01",
                    isRewatching = false,
                )
            fakeDao.allItemsFlow.value =
                listOf(UserAnimeListItem(anime = anime, userAnime = userAnime))

            val items = repository.observeUserAnimeList(ListFilterCategory.ALL).first()

            assertEquals(1, items.size)
            assertEquals("Steins;Gate", items[0].displayTitle)
        }

    @Suppress("SameParameterValue")
    private fun createEntryDto(
        id: Long,
        title: String,
        score: Int,
    ) = AnimeListEntryDto(
        node =
            AnimeNodeDto(
                id = id,
                title = title,
            ),
        listStatus =
            MyListStatusDto(
                status = "watching",
                score = score,
                numEpisodesWatched = 5,
                updatedAt = "2023-01-01",
                isRewatching = false,
            ),
    )

    private class FakeAnimeListRemoteDataSource : AnimeListRemoteDataSource {
        var fetchCallCount = 0
        var entries: List<AnimeListEntryDto> = emptyList()
        var exceptionToThrow: Throwable? = null

        override suspend fun fetchAllUserAnime(): List<AnimeListEntryDto> {
            fetchCallCount++
            exceptionToThrow?.let { throw it }
            return entries
        }
    }

    private class FakeSyncPreferences : SyncPreferences {
        var timestamp = 0L
        override val lastAnimeListSyncTimestamp: Flow<Long> = MutableStateFlow(0L)

        override suspend fun getLastAnimeListSyncTimestamp(): Long = timestamp

        override suspend fun updateLastAnimeListSyncTimestamp(timestamp: Long) {
            this.timestamp = timestamp
        }

        override suspend fun clearSyncPreferences() {
            timestamp = 0L
        }
    }

    private class FakeUserAnimeListDao : UserAnimeListDao {
        val allItemsFlow = MutableStateFlow<List<UserAnimeListItem>>(emptyList())
        var syncedAnimes = emptyList<AnimeEntity>()
        var syncedUserList = emptyList<UserAnimeListEntity>()

        override fun observeAllUserAnime(): Flow<List<UserAnimeListItem>> = allItemsFlow

        override fun observeUserAnimeByStatus(status: String): Flow<List<UserAnimeListItem>> = allItemsFlow

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) {
            syncedAnimes = animes
        }

        override suspend fun upsertUserAnimeList(userAnimeList: List<UserAnimeListEntity>) {
            syncedUserList = userAnimeList
        }

        override suspend fun clearUserAnimeList(): Int = 0

        override suspend fun syncUserAnimeList(
            animes: List<AnimeEntity>,
            userAnimeList: List<UserAnimeListEntity>,
        ) {
            syncedAnimes = animes
            syncedUserList = userAnimeList
        }
    }

    companion object {
        private const val COOLDOWN_EXPIRED_OFFSET = 1_000_000L
    }
}
