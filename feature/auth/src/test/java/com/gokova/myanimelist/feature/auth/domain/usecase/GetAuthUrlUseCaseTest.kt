package com.gokova.myanimelist.feature.auth.domain.usecase

import com.gokova.myanimelist.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetAuthUrlUseCaseTest {
    private lateinit var repository: FakeAuthRepository
    private lateinit var useCase: GetAuthUrlUseCase

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        useCase = GetAuthUrlUseCase(repository)
    }

    @Test
    fun `invoke returns url from repository`() =
        runTest {
            val expectedUrl = "https://myanimelist.net/v1/oauth2/authorize?sample=true"
            repository.authUrlToReturn = expectedUrl

            val result = useCase()

            assertEquals(expectedUrl, result)
        }
}
