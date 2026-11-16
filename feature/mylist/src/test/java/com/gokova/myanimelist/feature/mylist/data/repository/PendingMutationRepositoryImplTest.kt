package com.gokova.myanimelist.feature.mylist.data.repository

import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.database.util.UserAnimeListSyncCoordinator
import com.gokova.myanimelist.core.domain.model.ConflictResolution
import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeDetailsDto
import com.gokova.myanimelist.core.network.model.MyListStatusDto
import com.gokova.myanimelist.feature.mylist.testing.FakePendingMutationScheduler
import com.gokova.myanimelist.feature.mylist.testing.FakePendingUserAnimeMutationDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@Suppress("TooManyFunctions")
class PendingMutationRepositoryImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var pendingMutationDao: FakePendingUserAnimeMutationDao
    private lateinit var userAnimeListDao: FakeUserAnimeListDao
    private lateinit var malApiService: FakeMalApiService
    private lateinit var scheduler: FakePendingMutationScheduler
    private lateinit var repository: PendingMutationRepositoryImpl

    @Before
    fun setUp() {
        pendingMutationDao = FakePendingUserAnimeMutationDao()
        userAnimeListDao = FakeUserAnimeListDao()
        malApiService = FakeMalApiService()
        scheduler = FakePendingMutationScheduler()
        repository =
            PendingMutationRepositoryImpl(
                pendingMutationDao = pendingMutationDao,
                userAnimeListDao = userAnimeListDao,
                malApiService = malApiService,
                pendingMutationScheduler = scheduler,
                syncCoordinator = UserAnimeListSyncCoordinator(),
                ioDispatcher = testDispatcher,
            )
    }

    @Test
    fun `saveMutationLocally saves mutation and updates user anime list optimistically`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)

            val params =
                UpdateUserAnimeStatusParams(
                    status = "watching",
                    numWatchedEpisodes = 4,
                    score = 8,
                )

            val result = repository.saveMutationLocally(animeId, params)

            assertTrue(result.isSuccess)
            val pending = pendingMutationDao.getPendingMutationById(animeId)
            assertNotNull(pending)
            assertEquals("watching", pending?.status)
            assertEquals(4, pending?.numEpisodesWatched)
            assertEquals(8, pending?.score)

            // Local cache updated optimistically
            val updatedUserAnime = userAnimeListDao.getUserAnimeById(animeId)?.userAnime
            assertEquals(4, updatedUserAnime?.numEpisodesWatched)
            assertEquals(8, updatedUserAnime?.score)
        }

    @Test
    fun `saveMutationLocally coalesces sequential edits while preserving baseline`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)

            // Edit 1: watch episode 4
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 4),
            )
            // Edit 2: watch episode 5 and rate 9
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 5, score = 9),
            )

            val pending = pendingMutationDao.getPendingMutationById(animeId)
            assertNotNull(pending)
            // Baseline preserved from original state
            assertEquals(3, pending?.baselineEpisodes)
            assertEquals(7, pending?.baselineScore)
            // Current desired state reflects coalesced value
            assertEquals(5, pending?.numEpisodesWatched)
            assertEquals(9, pending?.score)
        }

    @Test
    fun `syncMutation on success updates remote and deletes pending mutation`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 4),
            )

            malApiService.detailsDto =
                AnimeDetailsDto(
                    id = animeId,
                    title = "Test Anime",
                    myListStatus = MyListStatusDto(status = "watching", numEpisodesWatched = 3),
                )

            val result = repository.syncMutation(animeId)

            assertTrue(result.isSuccess)
            assertNull(pendingMutationDao.getPendingMutationById(animeId))
            assertEquals(animeId, malApiService.lastUpdatedAnimeId)
            assertEquals(4, malApiService.lastEpisodes)
        }

    @Test
    fun `syncMutation on 401 updates state to AUTH_REQUIRED`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 4),
            )

            val http401 =
                HttpException(Response.error<Any>(401, "".toResponseBody(null)))
            malApiService.detailsException = http401

            val result = repository.syncMutation(animeId)

            assertTrue(result.isFailure)
            val pending = pendingMutationDao.getPendingMutationById(animeId)
            assertEquals("AUTH_REQUIRED", pending?.syncState)
        }

    @Test
    fun `syncMutation on network failure keeps state as PENDING`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 4),
            )

            malApiService.detailsException = IOException("No internet connection")

            val result = repository.syncMutation(animeId)

            assertTrue(result.isFailure)
            val pending = pendingMutationDao.getPendingMutationById(animeId)
            assertEquals("PENDING", pending?.syncState)
        }

    @Test
    fun `syncMutation on HTTP error updates state to FAILED`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 4),
            )

            val mediaType = "application/json".toMediaType()
            val errorResponse =
                Response.error<AnimeDetailsDto>(
                    500,
                    "{\"error\":\"Internal error\"}".toResponseBody(mediaType),
                )
            malApiService.detailsException = HttpException(errorResponse)

            val result = repository.syncMutation(animeId)

            assertTrue(result.isFailure)
            val pending = pendingMutationDao.getPendingMutationById(animeId)
            assertEquals("FAILED", pending?.syncState)
        }

    @Test
    fun `syncMutation with identical remote state completes and deletes outbox`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 4),
            )

            // Remote already has episode 4
            malApiService.detailsDto =
                AnimeDetailsDto(
                    id = animeId,
                    title = "Test Anime",
                    myListStatus =
                        MyListStatusDto(
                            status = "watching",
                            numEpisodesWatched = 4,
                            score = 7,
                        ),
                )

            val result = repository.syncMutation(animeId)

            assertTrue(result.isSuccess)
            assertNull(pendingMutationDao.getPendingMutationById(animeId))
            // No PUT was needed since remote was already identical
            assertNull(malApiService.lastUpdatedAnimeId)
        }

    @Test
    fun `syncMutation with conflicting remote changes flags CONFLICT`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 5),
            )

            // Remote was edited on web to episode 8 (diverging from baseline 3)
            malApiService.detailsDto =
                AnimeDetailsDto(
                    id = animeId,
                    title = "Test Anime",
                    myListStatus =
                        MyListStatusDto(
                            status = "watching",
                            numEpisodesWatched = 8,
                            score = 7,
                        ),
                )

            val result = repository.syncMutation(animeId)

            assertTrue(result.isFailure)
            val pending = pendingMutationDao.getPendingMutationById(animeId)
            assertEquals("CONFLICT", pending?.syncState)
            assertEquals(8, pending?.remoteEpisodes)
        }

    @Test
    fun `resolveConflict KeepDevice forces local values to MAL and clears outbox`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 5),
            )

            val result =
                repository.resolveConflict(
                    animeId = animeId,
                    resolution = ConflictResolution.KeepDevice,
                )

            assertTrue(result.isSuccess)
            assertNull(pendingMutationDao.getPendingMutationById(animeId))
            assertEquals(5, malApiService.lastEpisodes)
        }

    @Test
    fun `resolveConflict UseRemote updates local Room with remote values and clears outbox`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 5),
            )

            // Simulate remote conflict values
            pendingMutationDao.updateConflict(
                animeId = animeId,
                remoteStatus = "completed",
                remoteEpisodes = 12,
                remoteScore = 10,
                remoteIsRewatching = false,
                syncState = "CONFLICT",
            )

            val result =
                repository.resolveConflict(
                    animeId = animeId,
                    resolution = ConflictResolution.UseRemote,
                )

            assertTrue(result.isSuccess)
            assertNull(pendingMutationDao.getPendingMutationById(animeId))
            val localItem = userAnimeListDao.getUserAnimeById(animeId)?.userAnime
            assertEquals("completed", localItem?.status)
            assertEquals(12, localItem?.numEpisodesWatched)
            assertEquals(10, localItem?.score)
        }

    @Test
    fun `resolveConflict UseRemote does not overwrite if newer local edit was saved`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 5),
            )

            // Simulate remote conflict state
            pendingMutationDao.updateConflict(
                animeId = animeId,
                remoteStatus = "completed",
                remoteEpisodes = 12,
                remoteScore = 10,
                remoteIsRewatching = false,
                syncState = "CONFLICT",
            )

            // Simulate user saving another edit locally before resolving
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 6),
            )

            val result =
                repository.resolveConflict(
                    animeId = animeId,
                    resolution = ConflictResolution.UseRemote,
                )

            assertTrue(result.isSuccess)
            val pending = pendingMutationDao.getPendingMutationById(animeId)
            assertNotNull(pending)
            assertEquals(6, pending?.numEpisodesWatched)
            assertEquals("PENDING", pending?.syncState)

            val localItem = userAnimeListDao.getUserAnimeById(animeId)?.userAnime
            assertEquals(6, localItem?.numEpisodesWatched)
        }

    @Test
    fun `discardMutation reverts local entity to remote or baseline values and deletes outbox`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 5),
            )

            val result = repository.discardMutation(animeId)

            assertTrue(result.isSuccess)
            assertNull(pendingMutationDao.getPendingMutationById(animeId))
            // Reverted back to baseline 3
            val localItem = userAnimeListDao.getUserAnimeById(animeId)?.userAnime
            assertEquals(3, localItem?.numEpisodesWatched)
        }

    @Test
    fun `clearAllPendingMutations clears all entries`() =
        runTest(testDispatcher) {
            val animeId1 = 1L
            val animeId2 = 2L
            userAnimeListDao.items[animeId1] = createUserAnimeListItem(animeId1, "watching", 3, 7)
            userAnimeListDao.items[animeId2] = createUserAnimeListItem(animeId2, "watching", 1, 0)
            repository.saveMutationLocally(
                animeId1,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 4),
            )
            repository.saveMutationLocally(
                animeId2,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 2),
            )

            assertEquals(2, repository.getPendingCount())

            val result = repository.clearAllPendingMutations()

            assertTrue(result.isSuccess)
            assertEquals(0, repository.getPendingCount())
        }

    @Test
    fun `resolveConflict CustomEdit saves edit without deadlock and schedules sync`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 5),
            )

            val result =
                repository.resolveConflict(
                    animeId = animeId,
                    resolution =
                        ConflictResolution.CustomEdit(
                            UpdateUserAnimeStatusParams(numWatchedEpisodes = 6),
                        ),
                )

            assertTrue(result.isSuccess)
            val updated = userAnimeListDao.getUserAnimeById(animeId)?.userAnime
            assertEquals(6, updated?.numEpisodesWatched)
            assertTrue(scheduler.scheduleCallCount > 0)
        }

    @Test
    fun `syncMutation sends only device modified fields during auto-merge`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            // Device modified only episodes from 3 to 4, score is unchanged
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 4),
            )

            // Remote modified score from 7 to 9 while device was offline
            malApiService.detailsDto =
                AnimeDetailsDto(
                    id = animeId,
                    title = "Test Anime",
                    myListStatus =
                        MyListStatusDto(
                            status = "watching",
                            numEpisodesWatched = 3,
                            score = 9,
                        ),
                )

            val result = repository.syncMutation(animeId)

            assertTrue(result.isSuccess)
            assertEquals(animeId, malApiService.lastUpdatedAnimeId)
            // Only numWatchedEpisodes was sent; score and status were null to avoid overwriting MAL
            assertEquals(4, malApiService.lastEpisodes)
            assertNull(malApiService.lastScore)
            assertNull(malApiService.lastStatus)
        }

    @Test
    fun `resetAuthRequiredMutations resets entries and schedules sync`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)
            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 4),
            )

            val mediaType = "application/json".toMediaType()
            val errorResponse =
                Response.error<AnimeDetailsDto>(
                    401,
                    "{\"error\":\"Unauthorized\"}".toResponseBody(mediaType),
                )
            malApiService.detailsException = HttpException(errorResponse)
            repository.syncMutation(animeId)

            val beforeReset = pendingMutationDao.getPendingMutationById(animeId)
            assertEquals("AUTH_REQUIRED", beforeReset?.syncState)

            val result = repository.resetAuthRequiredMutations()

            assertTrue(result.isSuccess)
            val afterReset = pendingMutationDao.getPendingMutationById(animeId)
            assertEquals("PENDING", afterReset?.syncState)
            assertTrue(scheduler.scheduleCallCount > 0)
        }

    @Test
    fun `syncAllPending prioritizes retryable network error over conflict`() =
        runTest(testDispatcher) {
            val anime1 = 1L
            val anime2 = 2L
            userAnimeListDao.items[anime1] = createUserAnimeListItem(anime1, "watching", 3, 7)
            userAnimeListDao.items[anime2] = createUserAnimeListItem(anime2, "watching", 5, 8)

            repository.saveMutationLocally(
                anime1,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 4),
            )
            repository.saveMutationLocally(
                anime2,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 6),
            )

            malApiService.detailsHandler = { id ->
                if (id == anime1) {
                    AnimeDetailsDto(
                        id = anime1,
                        title = "Anime 1",
                        myListStatus =
                            MyListStatusDto(
                                status = "watching",
                                numEpisodesWatched = 10,
                                score = 9,
                            ),
                    )
                } else {
                    throw IOException("Network unreachable")
                }
            }

            val result = repository.syncAllPending()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IOException)

            val mutation1 = pendingMutationDao.getPendingMutationById(anime1)
            assertEquals("CONFLICT", mutation1?.syncState)

            val mutation2 = pendingMutationDao.getPendingMutationById(anime2)
            assertEquals("PENDING", mutation2?.syncState)
        }

    @Test
    fun `concurrent local edit during in-flight sync preserves user edits`() =
        runTest(testDispatcher) {
            val animeId = 1L
            userAnimeListDao.items[animeId] = createUserAnimeListItem(animeId, "watching", 3, 7)

            repository.saveMutationLocally(
                animeId,
                UpdateUserAnimeStatusParams(numWatchedEpisodes = 4),
            )

            malApiService.detailsDto =
                AnimeDetailsDto(
                    id = animeId,
                    title = "Test Anime",
                    myListStatus =
                        MyListStatusDto(
                            status = "watching",
                            numEpisodesWatched = 3,
                            score = 7,
                        ),
                )

            // Simulate user performing another local edit while network update is in-flight
            malApiService.onUpdateCallback = {
                repository.saveMutationLocally(
                    animeId,
                    UpdateUserAnimeStatusParams(numWatchedEpisodes = 5),
                )
            }

            val result = repository.syncMutation(animeId)

            assertTrue(result.isSuccess)
            val userAnime = userAnimeListDao.getUserAnimeById(animeId)?.userAnime
            assertEquals(5, userAnime?.numEpisodesWatched)

            val remainingPending = pendingMutationDao.getPendingMutationById(animeId)
            assertNotNull(remainingPending)
            assertEquals(5, remainingPending?.numEpisodesWatched)
            assertEquals("PENDING", remainingPending?.syncState)
        }

    private fun createUserAnimeListItem(
        animeId: Long,
        status: String,
        episodesWatched: Int,
        score: Int,
    ) = UserAnimeListItem(
        anime =
            AnimeEntity(
                id = animeId,
                title = "Test Anime $animeId",
                titleEnglish = null,
                mainPictureMedium = null,
                mainPictureLarge = null,
                mediaType = null,
                airingStatus = null,
                numEpisodes = 12,
                startSeasonYear = null,
                startSeasonSeason = null,
                meanScore = null,
            ),
        userAnime =
            UserAnimeListEntity(
                animeId = animeId,
                status = status,
                score = score,
                numEpisodesWatched = episodesWatched,
                isRewatching = false,
                updatedAt = "2026-10-19T10:00:00Z",
            ),
    )

    private class FakeUserAnimeListDao : UserAnimeListDao {
        val items = mutableMapOf<Long, UserAnimeListItem>()

        override suspend fun getUserAnimeById(animeId: Long): UserAnimeListItem? = items[animeId]

        override fun observeUserAnimeById(animeId: Long): Flow<UserAnimeListItem?> =
            flowOf(items[animeId])

        override suspend fun upsertUserAnimeList(userAnimeList: List<UserAnimeListEntity>) {
            userAnimeList.forEach { entity ->
                val existing = items[entity.animeId]
                if (existing != null) {
                    items[entity.animeId] = existing.copy(userAnime = entity)
                }
            }
        }

        override suspend fun addAnimeToUserListAndClean(userAnime: UserAnimeListEntity) {
            val existing = items[userAnime.animeId]
            if (existing != null) {
                items[userAnime.animeId] = existing.copy(userAnime = userAnime)
            }
        }

        override suspend fun deleteUserAnime(animeId: Long): Int =
            if (items.remove(animeId) !=
                null
            ) {
                1
            } else {
                0
            }

        override suspend fun getPendingMutationAnimeIds(): List<Long> = emptyList()

        override suspend fun deleteUserAnimeByIds(ids: List<Long>): Int = 0

        override fun observeAllUserAnime(): Flow<List<UserAnimeListItem>> =
            flowOf(items.values.toList())

        override suspend fun getAllUserAnime(): List<UserAnimeListItem> = items.values.toList()

        override fun observeUserAnimeByStatus(status: String): Flow<List<UserAnimeListItem>> =
            flowOf(items.values.filter { it.userAnime.status == status })

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) = Unit

        override suspend fun clearUserAnimeList(): Int {
            val count = items.size
            items.clear()
            return count
        }

        override fun observeAnimeEntityById(animeId: Long): Flow<AnimeEntity?> =
            flowOf(items[animeId]?.anime)

        override suspend fun getAnimeEntityById(animeId: Long): AnimeEntity? = items[animeId]?.anime

        override suspend fun deleteRecommendation(animeId: Long): Int = 0

        override suspend fun deleteNewSeasonAnime(animeId: Long): Int = 0

        override suspend fun deleteIgnoredAnime(animeId: Long): Int = 0
    }

    private class FakeMalApiService : MalApiService {
        var detailsDto: AnimeDetailsDto? = null
        var detailsException: Throwable? = null
        var detailsHandler: ((Long) -> AnimeDetailsDto)? = null
        var onUpdateCallback: (suspend () -> Unit)? = null
        var lastUpdatedAnimeId: Long? = null
        var lastEpisodes: Int? = null
        var lastScore: Int? = null
        var lastStatus: String? = null
        var lastRewatching: Boolean? = null

        override suspend fun getAnimeDetails(
            animeId: Long,
            fields: String,
        ): AnimeDetailsDto {
            detailsHandler?.let { return it(animeId) }
            detailsException?.let { throw it }
            return detailsDto ?: AnimeDetailsDto(id = animeId, title = "Test Anime $animeId")
        }

        override suspend fun updateMyListStatus(
            animeId: Long,
            status: String?,
            isRewatching: Boolean?,
            score: Int?,
            numWatchedEpisodes: Int?,
        ): MyListStatusDto {
            onUpdateCallback?.invoke()
            lastUpdatedAnimeId = animeId
            lastStatus = status
            lastRewatching = isRewatching
            lastScore = score
            lastEpisodes = numWatchedEpisodes
            return MyListStatusDto(
                status = status ?: "watching",
                score = score ?: 0,
                numEpisodesWatched = numWatchedEpisodes ?: 0,
            )
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

        override suspend fun deleteMyListStatus(animeId: Long): Response<Unit> =
            throw UnsupportedOperationException()

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
}
