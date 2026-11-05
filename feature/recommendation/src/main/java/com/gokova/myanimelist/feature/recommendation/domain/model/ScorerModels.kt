package com.gokova.myanimelist.feature.recommendation.domain.model

data class UserTasteProfileItem(
    val animeId: Long,
    val genres: List<String>,
    val userScore: Int?,
)

data class CandidateAnimeItem(
    val animeId: Long,
    val genres: List<String>,
    val meanScore: Double?,
    val numListUsers: Int?,
    val numEpisodes: Int? = null,
    val mediaType: String? = null,
)

data class ScoredCandidateResult(
    val animeId: Long,
    val genreScore: Double,
    val genreRank: Int,
    val genreMatchPercent: Int,
    val themeScore: Double,
    val themeRank: Int,
    val themeMatchPercent: Int,
)

data class ScoredLabCandidateResult(
    val animeId: Long,
    val score: Double,
    val rank: Int,
    val matchPercent: Int,
    val breakdown: RecommendationScoreBreakdown,
)

data class EvaluationResult(
    val recommendations: List<ScoredCandidateResult>,
    val discardedAnimeIds: List<Long>,
)
