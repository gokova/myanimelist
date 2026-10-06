package com.gokova.myanimelist.feature.search.domain.repository

import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage

interface SearchRepository {
    suspend fun searchAnime(query: String): Result<SearchResultPage>

    suspend fun loadMore(nextUrl: String): Result<SearchResultPage>
}
