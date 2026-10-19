package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.model.UserAnimeListUpdate
import com.gokova.myanimelist.core.domain.repository.UserAnimeListStatusRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateAnimeStatusUseCaseTest {
    @Test
    fun `invoke delegates update to repository and returns success`() =
        runBlocking {
            val expectedUpdate =
                UserAnimeListUpdate(
                    animeId = 1L,
                    status = "watching",
                    score = 8,
                    numEpisodesWatched = 3,
                    isRewatching = false,
                    updatedAt = "2026-10-19",
                )
            val fakeRepo =
                object : UserAnimeListStatusRepository {
                    var capturedAnimeId: Long? = null
                    var capturedParams: UpdateUserAnimeStatusParams? = null

                    override suspend fun updateAnimeStatus(
                        animeId: Long,
                        params: UpdateUserAnimeStatusParams,
                    ): Result<UserAnimeListUpdate> {
                        capturedAnimeId = animeId
                        capturedParams = params
                        return Result.success(expectedUpdate)
                    }

                    override suspend fun deleteAnimeFromList(animeId: Long): Result<Unit> =
                        error("Not needed")
                }

            val useCase = UpdateAnimeStatusUseCase(fakeRepo)
            val params =
                UpdateUserAnimeStatusParams(
                    status = "watching",
                    score = 8,
                    numWatchedEpisodes = 3,
                )
            val result = useCase(animeId = 1L, params = params)

            assertTrue(result.isSuccess)
            assertEquals(expectedUpdate, result.getOrNull())
            assertEquals(1L, fakeRepo.capturedAnimeId)
            assertEquals(params, fakeRepo.capturedParams)
        }
}
