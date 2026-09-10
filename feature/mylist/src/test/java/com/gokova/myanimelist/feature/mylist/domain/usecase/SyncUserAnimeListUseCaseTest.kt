package com.gokova.myanimelist.feature.mylist.domain.usecase

import com.gokova.myanimelist.feature.mylist.domain.model.SyncStatus
import com.gokova.myanimelist.feature.mylist.testing.FakeMyListRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncUserAnimeListUseCaseTest {
    private lateinit var repository: FakeMyListRepository
    private lateinit var useCase: SyncUserAnimeListUseCase

    @Before
    fun setUp() {
        repository = FakeMyListRepository()
        useCase = SyncUserAnimeListUseCase(repository)
    }

    @Test
    fun `invoke delegates force flag to repository`() =
        runTest {
            val statuses = useCase(force = true).toList()

            assertEquals(listOf(SyncStatus.Started, SyncStatus.Completed), statuses)
            assertEquals(1, repository.syncCallCount)
            assertEquals(true, repository.lastForceFlag)
        }

    @Test
    fun `invoke returns failure when repository fails`() =
        runTest {
            repository.shouldFailSync = true

            val statuses = useCase(force = false).toList()

            assertEquals(2, statuses.size)
            assertEquals(SyncStatus.Started, statuses[0])
            assertTrue(statuses[1] is SyncStatus.Failure)
            assertEquals(1, repository.syncCallCount)
            assertEquals(false, repository.lastForceFlag)
        }
}
