package com.gokova.myanimelist.feature.search.data.remote

import com.gokova.myanimelist.core.network.model.AnimeListResponseDto

interface SearchRemoteDataSource {
    suspend fun searchAnime(query: String): AnimeListResponseDto

    suspend fun fetchNextPage(nextUrl: String): AnimeListResponseDto
}
