package com.gokova.myanimelist.feature.search.domain.model

data class AiSearchResult(
    val intentSummary: String,
    val explanation: String,
    val items: List<SearchAnimeItem>,
    val unverifiedCandidateTitles: List<String> = emptyList(),
)
