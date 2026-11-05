package com.gokova.myanimelist.feature.search.domain.model

data class SearchAnimeItem(
    val id: Long,
    val title: String,
    val englishTitle: String?,
    val displayTitle: String,
    val subtitleTitle: String?,
    val thumbnailUrl: String?,
    val largeImageUrl: String? = null,
    val mediaType: String?,
    val numEpisodes: Int?,
    val meanScore: Double?,
    val genres: List<String>,
    val userStatus: String?,
    val tasteMatchPercent: Int?,
    val aiReason: String? = null,
)
