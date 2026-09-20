package com.gokova.myanimelist.feature.recommendation.data.remote

import com.gokova.myanimelist.core.network.model.AnimeNodeDto

interface RecommendationRemoteDataSource {
    suspend fun fetchTopRankingAnime(limit: Int = DEFAULT_RANKING_LIMIT): List<AnimeNodeDto>

    suspend fun fetchSeasonalAnime(
        year: Int,
        season: String,
        limit: Int = DEFAULT_SEASONAL_LIMIT,
    ): List<AnimeNodeDto>

    companion object {
        const val DEFAULT_RANKING_LIMIT = 500
        const val DEFAULT_SEASONAL_LIMIT = 100
    }
}
