package com.gokova.myanimelist.feature.recommendation.domain.model

sealed interface DurationFitTier {
    data object SingleRelease : DurationFitTier

    data class ShortSeries(
        val episodes: Int,
    ) : DurationFitTier

    data class StandardCour(
        val episodes: Int,
    ) : DurationFitTier

    data class LongCommitment(
        val episodes: Int,
    ) : DurationFitTier
}

enum class PopularityTier {
    MAINSTREAM_FAVORITE,
    POPULAR_PICK,
    MODERATELY_KNOWN,
    NICHE_DISCOVERY,
}

data class RecommendationScoreBreakdown(
    val tasteAlignmentScore: Float,
    val discoveryScore: Float,
    val topMatchedTags: List<String>,
    val exploratoryTags: List<String>,
    val durationTier: DurationFitTier? = null,
    val popularityTier: PopularityTier? = null,
)
