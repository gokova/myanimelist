package com.gokova.myanimelist.feature.search.domain.model

data class SearchResultPage(
    val items: List<SearchAnimeItem> = emptyList(),
    val nextUrl: String? = null,
)
