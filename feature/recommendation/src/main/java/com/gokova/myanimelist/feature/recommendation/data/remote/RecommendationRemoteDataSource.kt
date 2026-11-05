package com.gokova.myanimelist.feature.recommendation.data.remote

import com.gokova.myanimelist.core.network.model.AnimeNodeDto

interface RecommendationRemoteDataSource {
    suspend fun fetchTopRankingAnime(
        rankingType: String = DEFAULT_RANKING_TYPE,
        limit: Int = DEFAULT_RANKING_LIMIT,
    ): List<AnimeNodeDto>

    suspend fun fetchSeasonalAnime(
        year: Int,
        season: String,
        limit: Int = DEFAULT_SEASONAL_LIMIT,
    ): List<AnimeNodeDto>

    companion object {
        const val DEFAULT_RANKING_TYPE = "all"
        const val RANKING_TYPE_MOVIE = "movie"
        const val DEFAULT_RANKING_LIMIT = 1500
        const val DEFAULT_MOVIE_LIMIT = 100
        const val DEFAULT_SEASONAL_LIMIT = 100
    }
}
