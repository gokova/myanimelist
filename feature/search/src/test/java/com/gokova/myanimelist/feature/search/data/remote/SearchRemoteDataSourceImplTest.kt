package com.gokova.myanimelist.feature.search.data.remote

import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeDetailsDto
import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.model.MyListStatusDto
import com.gokova.myanimelist.core.network.model.PagingDto
import com.gokova.myanimelist.core.network.model.UserDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SearchRemoteDataSourceImplTest {
    private lateinit var fakeApiService: FakeMalApiService
    private lateinit var dataSource: SearchRemoteDataSourceImpl

    @Before
    fun setUp() {
        fakeApiService = FakeMalApiService()
        dataSource = SearchRemoteDataSourceImpl(fakeApiService)
    }

    @Test
    fun searchAnimeCallsApiServiceAndReturnsResult() =
        runTest {
            val entry =
                AnimeListEntryDto(
                    node =
                        AnimeNodeDto(
                            id = 1L,
                            title = "Frieren",
                        ),
                )
            val expected =
                AnimeListResponseDto(
                    data = listOf(entry),
                    paging = PagingDto(next = "https://api.myanimelist.net/v2/anime?offset=20"),
                )
            fakeApiService.searchResult = expected

            val result = dataSource.searchAnime("Frieren")

            assertEquals("Frieren", fakeApiService.lastSearchQuery)
            assertEquals(expected, result)
        }

    @Test
    fun fetchNextPageCallsApiServiceAndReturnsResult() =
        runTest {
            val entry =
                AnimeListEntryDto(
                    node =
                        AnimeNodeDto(
                            id = 2L,
                            title = "Naruto",
                        ),
                )
            val expected =
                AnimeListResponseDto(
                    data = listOf(entry),
                )
            val testUrl = "https://api.myanimelist.net/v2/anime?offset=20"
            fakeApiService.nextPageResult = expected

            val result = dataSource.fetchNextPage(testUrl)

            assertEquals(testUrl, fakeApiService.lastNextPageUrl)
            assertEquals(expected, result)
        }

    @Test(expected = IOException::class)
    fun searchAnimePropagatesNetworkException() =
        runTest {
            fakeApiService.errorToThrow = IOException("No network")
            dataSource.searchAnime("Frieren")
        }

    private class FakeMalApiService : MalApiService {
        var searchResult = AnimeListResponseDto()
        var nextPageResult = AnimeListResponseDto()
        var errorToThrow: Throwable? = null
        var lastSearchQuery: String? = null
        var lastNextPageUrl: String? = null

        override suspend fun searchAnime(
            query: String,
            limit: Int,
            offset: Int,
            fields: String,
            nsfw: Boolean,
        ): AnimeListResponseDto {
            errorToThrow?.let { throw it }
            lastSearchQuery = query
            return searchResult
        }

        override suspend fun getAnimeListNextPage(url: String): AnimeListResponseDto {
            errorToThrow?.let { throw it }
            lastNextPageUrl = url
            return nextPageResult
        }

        override suspend fun getUserAnimeList(
            fields: String,
            limit: Int,
            offset: Int,
            nsfw: Boolean,
        ): AnimeListResponseDto = throw UnsupportedOperationException()

        override suspend fun getAnimeDetails(
            animeId: Long,
            fields: String,
        ): AnimeDetailsDto = throw UnsupportedOperationException()

        override suspend fun getAnimeRanking(
            rankingType: String,
            limit: Int,
            offset: Int,
            fields: String,
            nsfw: Boolean,
        ): AnimeListResponseDto = throw UnsupportedOperationException()

        override suspend fun getSeasonalAnime(
            year: Int,
            season: String,
            limit: Int,
            offset: Int,
            fields: String,
        ): AnimeListResponseDto = throw UnsupportedOperationException()

        override suspend fun updateMyListStatus(
            animeId: Long,
            status: String,
            numWatchedEpisodes: Int,
            score: Int,
        ): MyListStatusDto = throw UnsupportedOperationException()

        override suspend fun getUserProfile(
            userId: String,
            fields: String,
        ): UserDto = throw UnsupportedOperationException()
    }
}
