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

data class EvaluationResult(
    val recommendations: List<ScoredCandidateResult>,
    val discardedAnimeIds: List<Long>,
)
