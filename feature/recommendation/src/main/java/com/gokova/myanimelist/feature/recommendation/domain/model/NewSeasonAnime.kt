package com.gokova.myanimelist.feature.recommendation.domain.model

data class NewSeasonAnime(
    val animeId: Long,
    val displayTitle: String,
    val subtitleTitle: String?,
    val thumbnailUrl: String?,
    val largeImageUrl: String?,
    val mediaType: String?,
    val releaseSeason: String?,
    val airingStatus: String?,
    val score: Double?,
    val totalEpisodes: Int?,
    val parentAnimeId: Long,
    val parentTitle: String,
    val relationType: String,
    val relationTypeFormatted: String,
    val startSeasonYear: Int?,
    val startSeasonSeason: String?,
)
