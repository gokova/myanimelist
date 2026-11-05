package com.gokova.myanimelist.feature.recommendation.domain.model

enum class TimeCommitmentFilter(
    val maxEpisodes: Int?,
    val minEpisodes: Int?,
) {
    ANY(maxEpisodes = null, minEpisodes = null),
    SHORT(maxEpisodes = 13, minEpisodes = null),
    MEDIUM(maxEpisodes = 26, minEpisodes = 14),
    LONG(maxEpisodes = null, minEpisodes = 27),
    ;

    fun matches(numEpisodes: Int?): Boolean =
        when {
            this == ANY -> true
            numEpisodes == null || numEpisodes <= 0 -> false
            else -> {
                val maxOk = maxEpisodes == null || numEpisodes <= maxEpisodes
                val minOk = minEpisodes == null || numEpisodes >= minEpisodes
                maxOk && minOk
            }
        }
}
