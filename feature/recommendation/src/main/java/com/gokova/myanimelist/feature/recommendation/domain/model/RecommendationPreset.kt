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

    fun toConfig(excludedTags: Set<String> = emptySet()): RecommendationLabConfig =
        when (this) {
            COMFORT_ZONE ->
                buildConfig(
                    COMFORT_FAMILIARITY,
                    COMFORT_POPULARITY,
                    TimeCommitmentFilter.ANY,
                    COMFORT_MIN_SCORE,
                    excludedTags,
                )
            BALANCED ->
                buildConfig(
                    BALANCED_FAMILIARITY,
                    BALANCED_POPULARITY,
                    TimeCommitmentFilter.ANY,
                    BALANCED_MIN_SCORE,
                    excludedTags,
                )
            SERENDIPITY ->
                buildConfig(
                    SERENDIPITY_FAMILIARITY,
                    SERENDIPITY_POPULARITY,
                    TimeCommitmentFilter.ANY,
                    SERENDIPITY_MIN_SCORE,
                    excludedTags,
                )
            HIDDEN_GEMS ->
                buildConfig(
                    GEMS_FAMILIARITY,
                    GEMS_POPULARITY,
                    TimeCommitmentFilter.ANY,
                    GEMS_MIN_SCORE,
                    excludedTags,
                )
            WEEKEND_BINGE ->
                buildConfig(
                    WEEKEND_FAMILIARITY,
                    WEEKEND_POPULARITY,
                    TimeCommitmentFilter.SHORT,
                    WEEKEND_MIN_SCORE,
                    excludedTags,
                )
            LONG_COMMITMENT ->
                buildConfig(
                    LONG_FAMILIARITY,
                    LONG_POPULARITY,
                    TimeCommitmentFilter.LONG,
                    LONG_MIN_SCORE,
                    excludedTags,
                )
            CUSTOM ->
                RecommendationLabConfig(
                    preset = this,
                    excludedTags = excludedTags,
                )
        }

    private fun buildConfig(
        familiarity: Float,
        popularity: Float,
        timeCommitment: TimeCommitmentFilter,
        minCommunityScore: Double,
        excludedTags: Set<String>,
    ): RecommendationLabConfig =
        RecommendationLabConfig(
            preset = this,
            familiarity = familiarity,
            popularity = popularity,
            timeCommitment = timeCommitment,
            minCommunityScore = minCommunityScore,
            excludedTags = excludedTags,
        )

    companion object {
        private const val COMFORT_FAMILIARITY = 0.0f
        private const val COMFORT_POPULARITY = 0.15f
        private const val COMFORT_MIN_SCORE = 7.0

        private const val BALANCED_FAMILIARITY = 0.2f
        private const val BALANCED_POPULARITY = 0.5f
        private const val BALANCED_MIN_SCORE = 6.5

        private const val SERENDIPITY_FAMILIARITY = 0.7f
        private const val SERENDIPITY_POPULARITY = 0.5f
        private const val SERENDIPITY_MIN_SCORE = 7.0

        private const val GEMS_FAMILIARITY = 0.3f
        private const val GEMS_POPULARITY = 0.85f
        private const val GEMS_MIN_SCORE = 7.5

        private const val WEEKEND_FAMILIARITY = 0.2f
        private const val WEEKEND_POPULARITY = 0.5f
        private const val WEEKEND_MIN_SCORE = 7.0

        private const val LONG_FAMILIARITY = 0.1f
        private const val LONG_POPULARITY = 0.5f
        private const val LONG_MIN_SCORE = 7.0
    }
}
