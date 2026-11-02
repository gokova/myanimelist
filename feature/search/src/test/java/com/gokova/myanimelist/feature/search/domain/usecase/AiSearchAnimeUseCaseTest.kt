package com.gokova.myanimelist.feature.search.domain.usecase

import com.gokova.myanimelist.feature.search.domain.model.AiSearchResult
import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage
import com.gokova.myanimelist.feature.search.domain.repository.SearchRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class AiSearchAnimeUseCaseTest {
    private lateinit var fakeRepository: FakeSearchRepository
    private lateinit var useCase: AiSearchAnimeUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeSearchRepository()
        useCase = AiSearchAnimeUseCase(fakeRepository)
    }

    @Test
    fun blankPromptReturnsFailureWithoutCallingRepository() =
        runTest {
            val emptyResult = useCase("")
            assertTrue(emptyResult.isFailure)
            assertTrue(emptyResult.exceptionOrNull() is IllegalArgumentException)

            val whitespaceResult = useCase("   ")
            assertTrue(whitespaceResult.isFailure)
            assertTrue(whitespaceResult.exceptionOrNull() is IllegalArgumentException)
            assertEquals(0, fakeRepository.callCount)
        }

    @Test
    fun validPromptTrimsAndCallsRepository() =
        runTest {
            val expected =
                AiSearchResult(
                    intentSummary = "Dark Fantasy",
                    explanation = "Top picks",
                    items = emptyList(),
                )
            fakeRepository.result = Result.success(expected)

            val result = useCase("  psychological mystery  ")

            assertTrue(result.isSuccess)
            assertEquals(1, fakeRepository.callCount)
            assertEquals("psychological mystery", fakeRepository.lastPrompt)
            assertEquals(expected, result.getOrNull())
        }

    @Test
    fun repositoryErrorIsPropagated() =
        runTest {
            fakeRepository.result = Result.failure(IOException("Server timeout"))

            val result = useCase("action anime")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IOException)
        }

    private class FakeSearchRepository : SearchRepository {
        var result: Result<AiSearchResult> =
            Result.success(AiSearchResult("Summary", "Explanation", emptyList()))
        var callCount = 0
        var lastPrompt: String? = null

        override suspend fun searchAnime(query: String): Result<SearchResultPage> =
            Result.success(SearchResultPage())

        override suspend fun loadMore(nextUrl: String): Result<SearchResultPage> =
            Result.success(SearchResultPage())

        override suspend fun aiSearchAnime(prompt: String): Result<AiSearchResult> {
            callCount++
            lastPrompt = prompt
            return result
        }
    }
}
