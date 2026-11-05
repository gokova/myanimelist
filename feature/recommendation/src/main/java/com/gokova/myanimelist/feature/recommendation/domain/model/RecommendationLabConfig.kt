package com.gokova.myanimelist.feature.recommendation.domain.model

data class RecommendationLabConfig(
    val preset: RecommendationPreset = RecommendationPreset.BALANCED,
    val familiarity: Float = DEFAULT_FAMILIARITY,
    val popularity: Float = DEFAULT_POPULARITY,
    val timeCommitment: TimeCommitmentFilter = TimeCommitmentFilter.ANY,
    val minCommunityScore: Double? = DEFAULT_MIN_COMMUNITY_SCORE,
) {
    val isDefault: Boolean
        get() =
            preset == RecommendationPreset.BALANCED &&
                familiarity == DEFAULT_FAMILIARITY &&
                popularity == DEFAULT_POPULARITY &&
                timeCommitment == TimeCommitmentFilter.ANY &&
                minCommunityScore == DEFAULT_MIN_COMMUNITY_SCORE

    companion object {
        const val DEFAULT_FAMILIARITY = 0.2f
        const val DEFAULT_POPULARITY = 0.5f
        const val DEFAULT_MIN_COMMUNITY_SCORE = 6.5
    }
}
