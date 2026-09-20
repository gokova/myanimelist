package com.gokova.myanimelist.feature.recommendation.domain.model

data class RecommendedAnime(
    val animeId: Long,
    val title: String,
    val titleEnglish: String?,
    val imageUrl: String?,
    val mediaType: String?,
    val airingStatus: String?,
    val numEpisodes: Int?,
    val startSeasonYear: Int?,
    val startSeasonSeason: String?,
    val meanScore: Double?,
    val genreRank: Int,
    val genreScore: Double,
    val genreMatchPercent: Int,
    val themeRank: Int,
    val themeScore: Double,
    val themeMatchPercent: Int,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val source: String? = null,
) {
    fun rank(type: RecommendationType): Int =
        when (type) {
            RecommendationType.GENRE -> genreRank
            RecommendationType.THEME -> themeRank
        }

    fun matchPercent(type: RecommendationType): Int =
        when (type) {
            RecommendationType.GENRE -> genreMatchPercent
            RecommendationType.THEME -> themeMatchPercent
        }

    fun displayTitle(): String = titleEnglish ?: title
}
