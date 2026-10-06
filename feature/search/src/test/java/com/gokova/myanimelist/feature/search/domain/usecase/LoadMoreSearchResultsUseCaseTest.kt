package com.gokova.myanimelist.feature.search.domain.usecase

import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage
import com.gokova.myanimelist.feature.search.domain.repository.SearchRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class LoadMoreSearchResultsUseCaseTest {
    private lateinit var fakeRepository: FakeSearchRepository
    private lateinit var useCase: LoadMoreSearchResultsUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeSearchRepository()
        useCase = LoadMoreSearchResultsUseCase(fakeRepository)
    }

    @Test
    fun delegatesNextUrlToRepositoryAndReturnsSuccess() =
        runTest {
            val expected = SearchResultPage(items = emptyList(), nextUrl = null)
            fakeRepository.loadMoreResult = Result.success(expected)
            val url = "https://api.myanimelist.net/v2/anime?offset=20"

            val result = useCase(url)

            assertTrue(result.isSuccess)
            assertEquals(url, fakeRepository.lastLoadMoreUrl)
            assertEquals(expected, result.getOrNull())
        }

    @Test
    fun propagatesRepositoryFailure() =
        runTest {
            fakeRepository.loadMoreResult = Result.failure(IOException("Server error"))

            val result = useCase("https://api.myanimelist.net/v2/anime?offset=20")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IOException)
        }

    private class FakeSearchRepository : SearchRepository {
        var loadMoreResult: Result<SearchResultPage> = Result.success(SearchResultPage())
        var lastLoadMoreUrl: String? = null

        override suspend fun searchAnime(query: String): Result<SearchResultPage> =
            Result.success(SearchResultPage())

        override suspend fun loadMore(nextUrl: String): Result<SearchResultPage> {
            lastLoadMoreUrl = nextUrl
            return loadMoreResult
        }
    }
}
