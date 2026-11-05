package com.gokova.myanimelist.feature.recommendation.domain.algorithm

import com.gokova.myanimelist.core.domain.taxonomy.TagType
import com.gokova.myanimelist.core.domain.taxonomy.classifyTag
import com.gokova.myanimelist.feature.recommendation.domain.model.CandidateAnimeItem
import com.gokova.myanimelist.feature.recommendation.domain.model.DurationFitTier
import com.gokova.myanimelist.feature.recommendation.domain.model.EvaluationResult
import com.gokova.myanimelist.feature.recommendation.domain.model.PopularityTier
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationLabConfig
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationScoreBreakdown
import com.gokova.myanimelist.feature.recommendation.domain.model.ScoredCandidateResult
import com.gokova.myanimelist.feature.recommendation.domain.model.ScoredLabCandidateResult
import com.gokova.myanimelist.feature.recommendation.domain.model.UserTasteProfileItem
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Suppress("TooManyFunctions")
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

    fun evaluateLab(
        userList: List<UserTasteProfileItem>,
        candidates: List<CandidateAnimeItem>,
        config: RecommendationLabConfig,
        targetType: TagType,
        maxResults: Int = DEFAULT_MAX_LAB_RESULTS,
    ): List<ScoredLabCandidateResult> {
        val filtered = filterLabCandidates(candidates, config)
        if (userList.isEmpty() || filtered.isEmpty()) return emptyList()

        val poolSize = candidates.size.toDouble()
        val idfMap = calculateIdf(candidates, targetType, poolSize)
        val userWeights = calculateUserTagWeights(userList, targetType, idfMap)
        return if (userWeights.isEmpty()) {
            emptyList()
        } else {
            scoreAndRankLab(filtered, userWeights, config, targetType, maxResults)
        }
    }

    private fun filterLabCandidates(
        candidates: List<CandidateAnimeItem>,
        config: RecommendationLabConfig,
    ): List<CandidateAnimeItem> =
        candidates.filter { candidate ->
            val timeOk = config.timeCommitment.matches(candidate.numEpisodes)
            val scoreOk =
                config.minCommunityScore == null ||
                    (candidate.meanScore ?: DEFAULT_ZERO_SCORE) >= config.minCommunityScore
            val nicheUserOk =
                config.popularity <= POPULARITY_MEDIAN ||
                    (candidate.numListUsers ?: 0) >= MIN_NICHE_LIST_USERS
            timeOk && scoreOk && nicheUserOk
        }

    private fun scoreAndRankLab(
        filtered: List<CandidateAnimeItem>,
        userWeights: Map<String, Double>,
        config: RecommendationLabConfig,
        targetType: TagType,
        maxResults: Int,
    ): List<ScoredLabCandidateResult> {
        val userTags = userWeights.keys
        val avgWeight = if (userWeights.isNotEmpty()) userWeights.values.average() else 0.0
        val strongAnchorTags = userWeights.filterValues { it >= avgWeight * 0.5 }.keys
        val scoredItems = mutableListOf<Pair<CandidateAnimeItem, Double>>()
        val breakdowns = mutableMapOf<Long, RecommendationScoreBreakdown>()

        for (candidate in filtered) {
            val candidateTags = candidate.genres.filter { classifyTag(it) == targetType }
            val matchedTags = candidateTags.filter { it in userTags }
            val hasStrongAnchor = matchedTags.any { it in strongAnchorTags }
            val isEligible =
                matchedTags.isNotEmpty() &&
                    (config.familiarity <= POPULARITY_MEDIAN || hasStrongAnchor)
            if (!isEligible) continue

            val exploratoryTags = candidateTags.filter { it !in userTags }
            val affinity = sumCandidateAffinity(candidate.genres, targetType, userWeights)
            val explorationBonus =
                calculateExplorationBonus(
                    exploratoryTags = exploratoryTags,
                    candidateTags = candidateTags,
                    matchedTags = matchedTags,
                    userWeights = userWeights,
                    strongAnchorTags = strongAnchorTags,
                )

            val explorationWeighted =
                affinity * TASTE_FAMILIARITY_WEIGHT +
                    explorationBonus * TASTE_EXPLORATION_WEIGHT
            val tasteScore =
                (1.0 - config.familiarity) * affinity +
                    config.familiarity * explorationWeighted

            val popMultiplier =
                calculateLabPopularityMultiplier(
                    meanScore = candidate.meanScore,
                    numListUsers = candidate.numListUsers,
                    popularityPref = config.popularity,
                )

            val finalScore = tasteScore * popMultiplier
            scoredItems.add(candidate to finalScore)

            val details =
                ScoredCandidateDetails(
                    candidate = candidate,
                    affinity = affinity,
                    explorationBonus = explorationBonus,
                    matchedTags =
                        matchedTags.sortedByDescending {
                            userWeights[it] ?: DEFAULT_ZERO_SCORE
                        },
                    exploratoryTags = exploratoryTags,
                )
            breakdowns[candidate.animeId] = buildBreakdown(details, config)
        }

        return formatRankedResults(scoredItems, breakdowns, maxResults)
    }

    private fun formatRankedResults(
        scoredItems: List<Pair<CandidateAnimeItem, Double>>,
        breakdowns: Map<Long, RecommendationScoreBreakdown>,
        maxResults: Int,
    ): List<ScoredLabCandidateResult> {
        if (scoredItems.isEmpty()) return emptyList()

        val sorted = scoredItems.sortedByDescending { it.second }.take(maxResults)
        val maxScore = max(sorted.firstOrNull()?.second ?: DEFAULT_ZERO_SCORE, MIN_SCORE_THRESHOLD)

        return sorted.mapIndexed { index, (candidate, score) ->
            ScoredLabCandidateResult(
                animeId = candidate.animeId,
                score = score,
                rank = index + 1,
                matchPercent = calculateMatchPercent(score, maxScore),
                breakdown = breakdowns[candidate.animeId] ?: fallbackBreakdown(),
            )
        }
    }

    private fun calculateExplorationBonus(
        exploratoryTags: List<String>,
        candidateTags: List<String>,
        matchedTags: List<String>,
        userWeights: Map<String, Double>,
        strongAnchorTags: Set<String>,
    ): Double {
        if (exploratoryTags.isEmpty() || candidateTags.isEmpty()) return 0.0
        val anchorTag =
            matchedTags.filter { it in strongAnchorTags }.maxByOrNull { userWeights[it] ?: 0.0 }
        val anchorWeight = anchorTag?.let { userWeights[it] } ?: 0.0
        val expRatio = exploratoryTags.size.toDouble() / candidateTags.size.toDouble()
        return expRatio * anchorWeight
    }

    private fun calculateLabPopularityMultiplier(
        meanScore: Double?,
        numListUsers: Int?,
        popularityPref: Float,
    ): Double {
        val safeMean = (meanScore ?: DEFAULT_FALLBACK_MEAN_SCORE).coerceIn(1.0, 10.0)
        val scoreFactor = safeMean / 10.0
        val users = max((numListUsers ?: DEFAULT_MIN_USERS).toDouble(), 10.0)
        val popularityFactor = (log10(users) / 6.0).coerceIn(0.1, 1.0)

        return if (popularityPref <= POPULARITY_MEDIAN) {
            val mainstreamIntensity = (POPULARITY_MEDIAN - popularityPref) * 2.0
            val popWeight = POPULARITY_WEIGHT + (mainstreamIntensity * 0.4)
            val qualityWeight = 1.0 - popWeight
            qualityWeight * scoreFactor + popWeight * popularityFactor
        } else {
            val nicheIntensity = (popularityPref - POPULARITY_MEDIAN) * 2.0
            val obscurityFactor = (1.0 - popularityFactor).coerceIn(0.0, 0.9)
            val qualityWeight = QUALITY_WEIGHT_FLOOR + (nicheIntensity * 0.1)
            val nicheUserBonus = if (users < 3000.0) 0.8 else 1.0
            (qualityWeight * scoreFactor + (1.0 - qualityWeight) * obscurityFactor) * nicheUserBonus
        }
    }

    private fun buildBreakdown(
        details: ScoredCandidateDetails,
        config: RecommendationLabConfig,
    ): RecommendationScoreBreakdown {
        val tasteAlignment =
            (details.affinity * TASTE_ALIGNMENT_FACTOR).coerceIn(0.1, 1.0).toFloat()
        val discovery =
            if (details.exploratoryTags.isNotEmpty()) {
                (
                    details.explorationBonus * EXPLORATION_BONUS_FACTOR +
                        config.familiarity * FAMILIARITY_FACTOR
                ).coerceIn(0.1, 1.0).toFloat()
            } else {
                (config.familiarity * FAMILIARITY_LOW_FACTOR).coerceIn(0.0, 1.0).toFloat()
            }

        val durationTier = determineDurationTier(details.candidate.numEpisodes)
        val popularityTier = determinePopularityTier(details.candidate.numListUsers)

        return RecommendationScoreBreakdown(
            tasteAlignmentScore = tasteAlignment,
            discoveryScore = discovery,
            topMatchedTags = details.matchedTags.take(TOP_MATCHED_TAGS_LIMIT),
            exploratoryTags = details.exploratoryTags.take(EXPLORATORY_TAGS_LIMIT),
            durationTier = durationTier,
            popularityTier = popularityTier,
        )
    }

    private fun determineDurationTier(episodes: Int?): DurationFitTier? =
        when {
            episodes == 1 -> DurationFitTier.SingleRelease
            episodes != null && episodes <= EPISODES_SHORT_THRESHOLD ->
                DurationFitTier.ShortSeries(episodes)
            episodes != null &&
                episodes in (EPISODES_SHORT_THRESHOLD + 1)..EPISODES_STANDARD_MAX ->
                DurationFitTier.StandardCour(episodes)
            episodes != null ->
                DurationFitTier.LongCommitment(episodes)
            else -> null
        }

    private fun determinePopularityTier(users: Int?): PopularityTier =
        when {
            (users ?: 0) >= USERS_MAINSTREAM_THRESHOLD -> PopularityTier.MAINSTREAM_FAVORITE
            (users ?: 0) >= USERS_POPULAR_THRESHOLD -> PopularityTier.POPULAR_PICK
            (users ?: 0) >= USERS_MODERATE_THRESHOLD -> PopularityTier.MODERATELY_KNOWN
            else -> PopularityTier.NICHE_DISCOVERY
        }

    private fun fallbackBreakdown(): RecommendationScoreBreakdown =
        RecommendationScoreBreakdown(
            tasteAlignmentScore = 0.5f,
            discoveryScore = 0.0f,
            topMatchedTags = emptyList(),
            exploratoryTags = emptyList(),
            durationTier = null,
            popularityTier = null,
        )

    private data class ScoredCandidateDetails(
        val candidate: CandidateAnimeItem,
        val affinity: Double,
        val explorationBonus: Double,
        val matchedTags: List<String>,
        val exploratoryTags: List<String>,
    )

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
        private const val DEFAULT_MAX_LAB_RESULTS = 500
        private const val DEFAULT_ZERO_SCORE = 0.0
        private const val MIN_SCORE_THRESHOLD = 0.0001
        private const val TASTE_FAMILIARITY_WEIGHT = 0.4
        private const val TASTE_EXPLORATION_WEIGHT = 0.6
        private const val POPULARITY_MEDIAN = 0.5f
        private const val QUALITY_WEIGHT_FLOOR = 0.7
        private const val TASTE_ALIGNMENT_FACTOR = 2.5
        private const val EXPLORATION_BONUS_FACTOR = 3.0
        private const val FAMILIARITY_FACTOR = 0.5
        private const val FAMILIARITY_LOW_FACTOR = 0.3
        private const val TOP_MATCHED_TAGS_LIMIT = 3
        private const val EXPLORATORY_TAGS_LIMIT = 2
        private const val EPISODES_SHORT_THRESHOLD = 13
        private const val EPISODES_STANDARD_MAX = 26
        private const val USERS_MAINSTREAM_THRESHOLD = 300_000
        private const val USERS_POPULAR_THRESHOLD = 80_000
        private const val USERS_MODERATE_THRESHOLD = 10_000
        private const val MIN_NICHE_LIST_USERS = 5000
    }
}
