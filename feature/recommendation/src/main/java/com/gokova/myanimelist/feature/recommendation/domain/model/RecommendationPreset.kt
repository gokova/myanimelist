package com.gokova.myanimelist.feature.recommendation.domain.model

enum class RecommendationPreset {
    COMFORT_ZONE,
    BALANCED,
    SERENDIPITY,
    HIDDEN_GEMS,
    WEEKEND_BINGE,
    LONG_COMMITMENT,
    CUSTOM,
    ;

    fun toConfig(): RecommendationLabConfig =
        when (this) {
            COMFORT_ZONE ->
                RecommendationLabConfig(
                    preset = this,
                    familiarity = 0.0f,
                    popularity = 0.15f,
                    timeCommitment = TimeCommitmentFilter.ANY,
                    minCommunityScore = 7.0,
                )
            BALANCED ->
                RecommendationLabConfig(
                    preset = this,
                    familiarity = 0.2f,
                    popularity = 0.5f,
                    timeCommitment = TimeCommitmentFilter.ANY,
                    minCommunityScore = 6.5,
                )
            SERENDIPITY ->
                RecommendationLabConfig(
                    preset = this,
                    familiarity = 0.7f,
                    popularity = 0.5f,
                    timeCommitment = TimeCommitmentFilter.ANY,
                    minCommunityScore = 7.0,
                )
            HIDDEN_GEMS ->
                RecommendationLabConfig(
                    preset = this,
                    familiarity = 0.3f,
                    popularity = 0.85f,
                    timeCommitment = TimeCommitmentFilter.ANY,
                    minCommunityScore = 7.5,
                )
            WEEKEND_BINGE ->
                RecommendationLabConfig(
                    preset = this,
                    familiarity = 0.2f,
                    popularity = 0.5f,
                    timeCommitment = TimeCommitmentFilter.SHORT,
                    minCommunityScore = 7.0,
                )
            LONG_COMMITMENT ->
                RecommendationLabConfig(
                    preset = this,
                    familiarity = 0.1f,
                    popularity = 0.5f,
                    timeCommitment = TimeCommitmentFilter.LONG,
                    minCommunityScore = 7.0,
                )
            CUSTOM ->
                RecommendationLabConfig(preset = this)
        }
}
