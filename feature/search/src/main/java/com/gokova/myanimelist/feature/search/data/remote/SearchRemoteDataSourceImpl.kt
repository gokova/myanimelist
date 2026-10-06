package com.gokova.myanimelist.feature.search.data.remote

import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import com.gokova.myanimelist.core.network.util.executeWithRetry
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class SearchRemoteDataSourceImpl
    @Inject
    constructor(
        private val apiService: MalApiService,
    ) : SearchRemoteDataSource {
        override suspend fun searchAnime(query: String): AnimeListResponseDto =
            executeWithRetry {
                apiService.searchAnime(query = query)
            }

        override suspend fun fetchNextPage(nextUrl: String): AnimeListResponseDto {
            delay(PAGINATION_PACING_DELAY_MS.milliseconds)
            return executeWithRetry {
                apiService.getAnimeListNextPage(nextUrl)
            }
        }

        companion object {
            private const val PAGINATION_PACING_DELAY_MS = 500L
        }
    }
