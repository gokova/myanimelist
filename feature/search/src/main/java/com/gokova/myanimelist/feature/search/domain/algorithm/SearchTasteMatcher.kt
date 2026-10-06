package com.gokova.myanimelist.feature.search.domain.algorithm

import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.domain.taxonomy.TagType
import com.gokova.myanimelist.core.domain.taxonomy.classifyTag
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

class SearchTasteMatcher
    @Inject
    constructor() {
        fun calculateMatch(
            candidateGenres: List<String>,
            candidateMeanScore: Double?,
            candidateNumListUsers: Int?,
            userList: List<UserAnimeListItem>,
        ): Int? {
            if (userList.size < MIN_ANIME_COUNT || candidateGenres.isEmpty()) {
                return null
            }

            val userGenreWeights = calculateTagWeights(userList, TagType.GENRE)
            val userThemeWeights = calculateTagWeights(userList, TagType.THEME)

            val genreAffinity = sumAffinity(candidateGenres, TagType.GENRE, userGenreWeights)
            val themeAffinity = sumAffinity(candidateGenres, TagType.THEME, userThemeWeights)
            val combinedAffinity = GENRE_WEIGHT * genreAffinity + THEME_WEIGHT * themeAffinity

            return if (combinedAffinity > 0.0) {
                val quality = calculateQuality(candidateMeanScore, candidateNumListUsers)
                val rawScore = combinedAffinity * quality
                val normalized = (rawScore / BENCHMARK_MAX_SCORE).coerceIn(0.0, 1.0)
                val percent =
                    MIN_MATCH_PERCENT + ((MAX_MATCH_PERCENT - MIN_MATCH_PERCENT) * normalized)
                percent.roundToInt().coerceIn(MIN_MATCH_PERCENT, MAX_MATCH_PERCENT)
            } else {
                null
            }
        }

        private fun calculateTagWeights(
            userList: List<UserAnimeListItem>,
            targetType: TagType,
        ): Map<String, Double> {
            val rawWeights = mutableMapOf<String, Double>()
            var totalWeight = 0.0

            for (item in userList) {
                val score = item.userAnime.score
                val factor = if (score > 0) score / 10.0 else DEFAULT_UNRATED_SCORE_WEIGHT
                val tags = item.anime.genres?.map { it.name } ?: emptyList()
                val matchingTags = tags.filter { classifyTag(it) == targetType }

                for (tag in matchingTags) {
                    rawWeights[tag] = (rawWeights[tag] ?: 0.0) + factor
                    totalWeight += factor
                }
            }

            if (totalWeight <= 0.0) return emptyMap()

            return rawWeights.mapValues { (_, rawWeight) -> rawWeight / totalWeight }
        }

        private fun sumAffinity(
            candidateTags: List<String>,
            targetType: TagType,
            userWeights: Map<String, Double>,
        ): Double {
            val matchingTags = candidateTags.filter { classifyTag(it) == targetType }
            if (matchingTags.isEmpty()) return 0.0
            val sum = matchingTags.sumOf { userWeights[it] ?: 0.0 }
            return sum / sqrt(matchingTags.size.toDouble())
        }

        private fun calculateQuality(
            meanScore: Double?,
            numListUsers: Int?,
        ): Double {
            val safeMean = (meanScore ?: DEFAULT_FALLBACK_MEAN).coerceIn(1.0, 10.0)
            val scoreFactor = safeMean / 10.0
            val users = max((numListUsers ?: DEFAULT_MIN_USERS).toDouble(), 10.0)
            val popularityFactor = (log10(users) / 6.0).coerceIn(0.1, 1.0)
            return QUALITY_WEIGHT * scoreFactor + POPULARITY_WEIGHT * popularityFactor
        }

        companion object {
            const val MIN_ANIME_COUNT = 5
            private const val DEFAULT_UNRATED_SCORE_WEIGHT = 0.7
            private const val DEFAULT_FALLBACK_MEAN = 6.5
            private const val DEFAULT_MIN_USERS = 1000
            private const val QUALITY_WEIGHT = 0.7
            private const val POPULARITY_WEIGHT = 0.3
            private const val GENRE_WEIGHT = 0.6
            private const val THEME_WEIGHT = 0.4
            private const val BENCHMARK_MAX_SCORE = 0.15
            private const val MIN_MATCH_PERCENT = 10
            private const val MAX_MATCH_PERCENT = 99
        }
    }
