package com.gokova.myanimelist.feature.search.domain.usecase

import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage
import com.gokova.myanimelist.feature.search.domain.repository.SearchRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SearchAnimeUseCaseTest {
    private lateinit var fakeRepository: FakeSearchRepository
    private lateinit var useCase: SearchAnimeUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeSearchRepository()
        useCase = SearchAnimeUseCase(fakeRepository)
    }

    @Test
    fun queryWithFewerThan3CharactersReturnsFailure() =
        runTest {
            val resultShort = useCase("ab")
            assertTrue(resultShort.isFailure)
            assertTrue(resultShort.exceptionOrNull() is IllegalArgumentException)

            val resultWhitespace = useCase("  ab  ")
            assertTrue(resultWhitespace.isFailure)
            assertTrue(resultWhitespace.exceptionOrNull() is IllegalArgumentException)
        }

    @Test
    fun queryWith3OrMoreCharactersTrimsAndCallsRepository() =
        runTest {
            val expectedPage = SearchResultPage(items = emptyList(), nextUrl = null)
            fakeRepository.searchResult = Result.success(expectedPage)

            val result = useCase("  naruto  ")

            assertTrue(result.isSuccess)
            assertEquals("naruto", fakeRepository.lastSearchQuery)
            assertEquals(expectedPage, result.getOrNull())
        }

    @Test
    fun repositoryFailurePropagatedAsResultFailure() =
        runTest {
            fakeRepository.searchResult = Result.failure(IOException("Network error"))

            val result = useCase("naruto")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IOException)
        }

    private class FakeSearchRepository : SearchRepository {
        var searchResult: Result<SearchResultPage> = Result.success(SearchResultPage())
        var loadMoreResult: Result<SearchResultPage> = Result.success(SearchResultPage())
        var lastSearchQuery: String? = null
        var lastLoadMoreUrl: String? = null

        override suspend fun searchAnime(query: String): Result<SearchResultPage> {
            lastSearchQuery = query
            return searchResult
        }

        override suspend fun loadMore(nextUrl: String): Result<SearchResultPage> {
            lastLoadMoreUrl = nextUrl
            return loadMoreResult
        }
    }
}
