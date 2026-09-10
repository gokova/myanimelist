package com.gokova.myanimelist.feature.auth.domain.usecase

import com.gokova.myanimelist.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObserveAuthStateUseCaseTest {
    private lateinit var repository: FakeAuthRepository
    private lateinit var useCase: ObserveAuthStateUseCase

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        useCase = ObserveAuthStateUseCase(repository)
    }

    @Test
    fun `invoke reflects repository isLoggedIn state`() =
        runTest {
            assertFalse(useCase().first())

            repository.setLoggedIn(true)
            assertTrue(useCase().first())
        }
}
