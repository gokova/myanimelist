package com.gokova.myanimelist.feature.recommendation.domain.algorithm

import com.gokova.myanimelist.core.domain.taxonomy.TagType
import com.gokova.myanimelist.core.domain.taxonomy.classifyTag
import com.gokova.myanimelist.feature.recommendation.domain.model.CandidateAnimeItem
import com.gokova.myanimelist.feature.recommendation.domain.model.EvaluationResult
import com.gokova.myanimelist.feature.recommendation.domain.model.ScoredCandidateResult
import com.gokova.myanimelist.feature.recommendation.domain.model.UserTasteProfileItem
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

class RecommendationScorer {
    fun evaluate(
        userList: List<UserTasteProfileItem>,
        candidates: List<CandidateAnimeItem>,
        maxRetainedPerCategory: Int = 500,
    ): EvaluationResult {
        if (userList.isEmpty() || candidates.isEmpty()) {
            return EvaluationResult(emptyList(), candidates.map { it.animeId })
        }

        val poolSize = candidates.size.toDouble()
        val genreIdf = calculateIdf(candidates, TagType.GENRE, poolSize)
        val themeIdf = calculateIdf(candidates, TagType.THEME, poolSize)

        val userGenreWeights = calculateUserTagWeights(userList, TagType.GENRE, genreIdf)
        val userThemeWeights = calculateUserTagWeights(userList, TagType.THEME, themeIdf)

        val rawScores =
            candidates.map { candidate ->
                val quality =
                    calculateQualityMultiplier(
                        candidate.meanScore,
                        candidate.numListUsers,
                    )
                val genreAffinity =
                    sumCandidateAffinity(
                        candidate.genres,
                        TagType.GENRE,
                        userGenreWeights,
                    )
                val themeAffinity =
                    sumCandidateAffinity(
                        candidate.genres,
                        TagType.THEME,
                        userThemeWeights,
                    )
                RawScore(
                    animeId = candidate.animeId,
                    genreScore = genreAffinity * quality,
                    themeScore = themeAffinity * quality,
                )
            }

        return rankAndFilter(rawScores, maxRetainedPerCategory)
    }

    private fun calculateIdf(
        candidates: List<CandidateAnimeItem>,
        targetType: TagType,
        poolSize: Double,
    ): Map<String, Double> {
        val countMap = mutableMapOf<String, Int>()
        for (candidate in candidates) {
            val matchingTags = candidate.genres.filter { classifyTag(it) == targetType }.toSet()
            for (tag in matchingTags) {
                countMap[tag] = (countMap[tag] ?: 0) + 1
            }
        }
        return countMap.mapValues { (_, count) ->
            ln((poolSize + 1.0) / (count + 1.0)) + 1.0
        }
    }

    private fun calculateUserTagWeights(
        userList: List<UserTasteProfileItem>,
        targetType: TagType,
        idfMap: Map<String, Double>,
    ): Map<String, Double> {
        val rawWeights = mutableMapOf<String, Double>()
        var totalWeight = 0.0

        for (item in userList) {
            val ratingFactor =
                when {
                    item.userScore != null && item.userScore > 0 -> item.userScore / 10.0
                    else -> DEFAULT_UNRATED_SCORE_WEIGHT
                }
            val tags = item.genres.filter { classifyTag(it) == targetType }
            for (tag in tags) {
                rawWeights[tag] = (rawWeights[tag] ?: 0.0) + ratingFactor
                totalWeight += ratingFactor
            }
        }

        if (totalWeight <= 0.0) return emptyMap()

        return rawWeights.mapValues { (tag, rawWeight) ->
            val tf = rawWeight / totalWeight
            val idf = idfMap[tag] ?: 1.0
            tf * idf
        }
    }

    private fun calculateQualityMultiplier(
        meanScore: Double?,
        numListUsers: Int?,
    ): Double {
        val safeMean = (meanScore ?: DEFAULT_FALLBACK_MEAN_SCORE).coerceIn(1.0, 10.0)
        val scoreFactor = safeMean / 10.0
        val users = max((numListUsers ?: DEFAULT_MIN_USERS).toDouble(), 10.0)
        val popularityFactor = (log10(users) / 6.0).coerceIn(0.1, 1.0)
        return QUALITY_SCORE_WEIGHT * scoreFactor + POPULARITY_WEIGHT * popularityFactor
    }

    private fun sumCandidateAffinity(
        tags: List<String>,
        targetType: TagType,
        userWeights: Map<String, Double>,
    ): Double {
        val matchingTags = tags.filter { classifyTag(it) == targetType }
        if (matchingTags.isEmpty()) return 0.0
        val sum = matchingTags.sumOf { userWeights[it] ?: 0.0 }
        return sum / sqrt(matchingTags.size.toDouble())
    }

    private fun rankAndFilter(
        rawScores: List<RawScore>,
        maxRetainedPerCategory: Int,
    ): EvaluationResult {
        val maxGenre = max(rawScores.maxOfOrNull { it.genreScore } ?: 0.0, 0.0001)
        val maxTheme = max(rawScores.maxOfOrNull { it.themeScore } ?: 0.0, 0.0001)

        val byGenre = rawScores.sortedByDescending { it.genreScore }
        val genreRanks = byGenre.mapIndexed { idx, item -> item.animeId to (idx + 1) }.toMap()

        val byTheme = rawScores.sortedByDescending { it.themeScore }
        val themeRanks = byTheme.mapIndexed { idx, item -> item.animeId to (idx + 1) }.toMap()

        val retained = mutableListOf<ScoredCandidateResult>()
        val discardedIds = mutableListOf<Long>()

        for (item in rawScores) {
            val gRank = genreRanks[item.animeId] ?: Int.MAX_VALUE
            val tRank = themeRanks[item.animeId] ?: Int.MAX_VALUE

            if (gRank <= maxRetainedPerCategory || tRank <= maxRetainedPerCategory) {
                val gPercent = calculateMatchPercent(item.genreScore, maxGenre)
                val tPercent = calculateMatchPercent(item.themeScore, maxTheme)
                retained.add(
                    ScoredCandidateResult(
                        animeId = item.animeId,
                        genreScore = item.genreScore,
                        genreRank = gRank,
                        genreMatchPercent = gPercent,
                        themeScore = item.themeScore,
                        themeRank = tRank,
                        themeMatchPercent = tPercent,
                    ),
                )
            } else {
                discardedIds.add(item.animeId)
            }
        }

        return EvaluationResult(retained, discardedIds)
    }

    private fun calculateMatchPercent(
        score: Double,
        maxScore: Double,
    ): Int {
        if (score <= 0.0 || maxScore <= 0.0) return MIN_MATCH_PERCENT
        val ratio = score / maxScore
        val scaled = MIN_MATCH_PERCENT + ((MAX_MATCH_PERCENT - MIN_MATCH_PERCENT) * ratio)
        return scaled.roundToInt().coerceIn(MIN_MATCH_PERCENT, MAX_MATCH_PERCENT)
    }

    private data class RawScore(
        val animeId: Long,
        val genreScore: Double,
        val themeScore: Double,
    )

    companion object {
        private const val DEFAULT_UNRATED_SCORE_WEIGHT = 0.7
        private const val DEFAULT_FALLBACK_MEAN_SCORE = 6.5
        private const val DEFAULT_MIN_USERS = 1000
        private const val QUALITY_SCORE_WEIGHT = 0.7
        private const val POPULARITY_WEIGHT = 0.3
        private const val MIN_MATCH_PERCENT = 10
        private const val MAX_MATCH_PERCENT = 99
    }
}
