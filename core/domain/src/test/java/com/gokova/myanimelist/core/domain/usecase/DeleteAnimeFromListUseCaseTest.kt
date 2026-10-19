package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.model.UserAnimeListUpdate
import com.gokova.myanimelist.core.domain.repository.UserAnimeListStatusRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAnimeFromListUseCaseTest {
    @Test
    fun `invoke delegates deletion to repository and returns success`() =
        runBlocking {
            val fakeRepo =
                object : UserAnimeListStatusRepository {
                    var capturedAnimeId: Long? = null

                    override suspend fun updateAnimeStatus(
                        animeId: Long,
                        params: UpdateUserAnimeStatusParams,
                    ): Result<UserAnimeListUpdate> = error("Not needed")

                    override suspend fun deleteAnimeFromList(animeId: Long): Result<Unit> {
                        capturedAnimeId = animeId
                        return Result.success(Unit)
                    }
                }

            val useCase = DeleteAnimeFromListUseCase(fakeRepo)
            val result = useCase(animeId = 42L)

            assertTrue(result.isSuccess)
            assertEquals(42L, fakeRepo.capturedAnimeId)
        }
}
