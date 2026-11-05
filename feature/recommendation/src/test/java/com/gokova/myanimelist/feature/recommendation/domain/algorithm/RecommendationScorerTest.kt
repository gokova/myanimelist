package com.gokova.myanimelist.feature.recommendation.domain.algorithm

import com.gokova.myanimelist.core.domain.taxonomy.TagType
import com.gokova.myanimelist.feature.recommendation.domain.model.CandidateAnimeItem
import com.gokova.myanimelist.feature.recommendation.domain.model.DurationFitTier
import com.gokova.myanimelist.feature.recommendation.domain.model.PopularityTier
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationLabConfig
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationPreset
import com.gokova.myanimelist.feature.recommendation.domain.model.TimeCommitmentFilter
import com.gokova.myanimelist.feature.recommendation.domain.model.UserTasteProfileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationScorerTest {
    private val scorer = RecommendationScorer()

    @Test
    fun `evaluate with empty user list returns empty recommendations and discards candidates`() {
        val candidates =
            listOf(
                CandidateAnimeItem(1L, listOf("Action"), 8.0, 50000),
                CandidateAnimeItem(2L, listOf("Comedy"), 7.5, 30000),
            )

        val result = scorer.evaluate(emptyList(), candidates)

        assertTrue(result.recommendations.isEmpty())
        assertEquals(listOf(1L, 2L), result.discardedAnimeIds)
    }

    @Test
    fun `evaluate with empty candidates returns empty result`() {
        val userList =
            listOf(
                UserTasteProfileItem(100L, listOf("Action"), 8),
            )

        val result = scorer.evaluate(userList, emptyList())

        assertTrue(result.recommendations.isEmpty())
        assertTrue(result.discardedAnimeIds.isEmpty())
    }

    @Test
    fun `evaluate downweights ubiquitous tags and boosts niche tags matching user taste`() {
        val userList =
            listOf(
                UserTasteProfileItem(10L, listOf("Action"), 6),
                UserTasteProfileItem(11L, listOf("Action"), 7),
                UserTasteProfileItem(12L, listOf("Action"), 7),
                UserTasteProfileItem(20L, listOf("School"), 10),
                UserTasteProfileItem(21L, listOf("School"), 9),
            )

        val actionCandidates =
            (1L..10L).map { id ->
                CandidateAnimeItem(id, listOf("Action"), 7.5, 100000)
            }
        val schoolCandidate = CandidateAnimeItem(99L, listOf("School"), 8.5, 50000)
        val candidates = actionCandidates + schoolCandidate

        val result = scorer.evaluate(userList, candidates)

        val schoolResult = result.recommendations.first { it.animeId == 99L }
        assertEquals(1, schoolResult.themeRank)
        assertTrue(schoolResult.themeMatchPercent >= 90)
    }

    @Test
    fun `evaluate clamps match percentage between 10 and 99`() {
        val userList =
            listOf(
                UserTasteProfileItem(10L, listOf("Action"), 10),
            )
        val candidates =
            listOf(
                CandidateAnimeItem(1L, listOf("Action"), 9.5, 500000),
                CandidateAnimeItem(2L, listOf("Horror"), 5.0, 1000),
            )

        val result = scorer.evaluate(userList, candidates)

        for (item in result.recommendations) {
            assertTrue(item.genreMatchPercent in 10..99)
            assertTrue(item.themeMatchPercent in 10..99)
        }
    }

    @Test
    fun `evaluate discards candidates outside max retained threshold`() {
        val userList =
            listOf(
                UserTasteProfileItem(10L, listOf("Action"), 8),
            )
        val candidates =
            (1L..10L).map { id ->
                CandidateAnimeItem(id, listOf("Action"), 8.0 - (id * 0.1), 10000)
            }

        val result = scorer.evaluate(userList, candidates, maxRetainedPerCategory = 3)

        assertEquals(3, result.recommendations.size)
        assertEquals(7, result.discardedAnimeIds.size)
    }

    @Test
    fun `evaluateLab filters out candidates exceeding time commitment`() {
        val userList = listOf(UserTasteProfileItem(1L, listOf("Action"), 9))
        val shortAnime = CandidateAnimeItem(10L, listOf("Action"), 8.0, 50000, numEpisodes = 12)
        val longAnime = CandidateAnimeItem(20L, listOf("Action"), 8.5, 60000, numEpisodes = 50)
        val config =
            RecommendationLabConfig(
                timeCommitment = TimeCommitmentFilter.SHORT,
                minCommunityScore = null,
            )

        val results =
            scorer.evaluateLab(
                userList = userList,
                candidates = listOf(shortAnime, longAnime),
                config = config,
                targetType = TagType.GENRE,
            )

        assertEquals(1, results.size)
        assertEquals(10L, results.first().animeId)
        assertEquals(DurationFitTier.ShortSeries(12), results.first().breakdown.durationTier)
    }

    @Test
    fun `evaluateLab filters out candidates below minimum score threshold`() {
        val userList = listOf(UserTasteProfileItem(1L, listOf("Action"), 9))
        val highRated = CandidateAnimeItem(10L, listOf("Action"), 8.2, 50000)
        val lowRated = CandidateAnimeItem(20L, listOf("Action"), 6.8, 50000)
        val config = RecommendationLabConfig(minCommunityScore = 7.5)

        val results =
            scorer.evaluateLab(
                userList = userList,
                candidates = listOf(highRated, lowRated),
                config = config,
                targetType = TagType.GENRE,
            )

        assertEquals(1, results.size)
        assertEquals(10L, results.first().animeId)
    }

    @Test
    fun `evaluateLab boosts exploratory candidates when familiarity is set to surprising`() {
        val userList = listOf(UserTasteProfileItem(1L, listOf("Sci-Fi"), 10))
        // Candidate 1: Pure familiar (only Sci-Fi)
        val pureFamiliar = CandidateAnimeItem(10L, listOf("Sci-Fi"), 8.0, 50000)
        // Candidate 2: Anchored with Sci-Fi, but explores Horror
        val anchoredExplorer = CandidateAnimeItem(20L, listOf("Sci-Fi", "Horror"), 8.0, 50000)

        val serendipityConfig = RecommendationPreset.SERENDIPITY.toConfig()
        val results =
            scorer.evaluateLab(
                userList = userList,
                candidates = listOf(pureFamiliar, anchoredExplorer),
                config = serendipityConfig,
                targetType = TagType.GENRE,
            )

        assertEquals(2, results.size)
        assertEquals(20L, results.first().animeId)
        assertTrue(
            results
                .first()
                .breakdown.exploratoryTags
                .contains("Horror"),
        )
        assertTrue(results.first().breakdown.discoveryScore > 0.3f)
    }

    @Test
    fun `evaluateLab boosts niche titles when hidden gems preset is selected`() {
        val userList = listOf(UserTasteProfileItem(1L, listOf("Action"), 10))
        // Mainstream hit: 1,000,000 users
        val mainstream = CandidateAnimeItem(10L, listOf("Action"), 8.0, 1000000)
        // Niche discovery: 8,000 users, same mean score
        val niche = CandidateAnimeItem(20L, listOf("Action"), 8.0, 8000)

        val hiddenGemsConfig = RecommendationPreset.HIDDEN_GEMS.toConfig()
        val results =
            scorer.evaluateLab(
                userList = userList,
                candidates = listOf(mainstream, niche),
                config = hiddenGemsConfig,
                targetType = TagType.GENRE,
            )

        assertEquals(2, results.size)
        assertEquals(20L, results.first().animeId)
        assertEquals(PopularityTier.NICHE_DISCOVERY, results.first().breakdown.popularityTier)
    }

    @Test
    fun `evaluateLab in niche mode filters out titles under 5000 users`() {
        val userList = listOf(UserTasteProfileItem(1L, listOf("Action"), 10))
        val lowUserCandidate = CandidateAnimeItem(10L, listOf("Action"), 8.0, 4000)
        val validNicheCandidate = CandidateAnimeItem(20L, listOf("Action"), 8.0, 6000)

        val hiddenGemsConfig = RecommendationPreset.HIDDEN_GEMS.toConfig()
        val results =
            scorer.evaluateLab(
                userList = userList,
                candidates = listOf(lowUserCandidate, validNicheCandidate),
                config = hiddenGemsConfig,
                targetType = TagType.GENRE,
            )

        assertEquals(1, results.size)
        assertEquals(20L, results.first().animeId)
    }

    @Test
    fun `evaluateLab in mainstream mode boosts titles with higher user count`() {
        val userList = listOf(UserTasteProfileItem(1L, listOf("Action"), 10))
        val mainstream = CandidateAnimeItem(10L, listOf("Action"), 8.0, 1000000)
        val niche = CandidateAnimeItem(20L, listOf("Action"), 8.0, 8000)

        val comfortZoneConfig = RecommendationPreset.COMFORT_ZONE.toConfig()
        val results =
            scorer.evaluateLab(
                userList = userList,
                candidates = listOf(mainstream, niche),
                config = comfortZoneConfig,
                targetType = TagType.GENRE,
            )

        assertEquals(2, results.size)
        assertEquals(10L, results.first().animeId)
    }

    @Test
    fun `evaluateLab in surprising mode requires a strong anchor tag`() {
        val userList =
            listOf(
                UserTasteProfileItem(1L, listOf("Sci-Fi"), 10),
                UserTasteProfileItem(2L, listOf("Sci-Fi"), 10),
                UserTasteProfileItem(3L, listOf("Sci-Fi"), 10),
                UserTasteProfileItem(4L, listOf("Sci-Fi"), 10),
                UserTasteProfileItem(5L, listOf("Horror"), 1),
            )
        // Strong anchor (Sci-Fi) + exploratory (Comedy)
        val strongAnchor = CandidateAnimeItem(10L, listOf("Sci-Fi", "Comedy"), 8.0, 50000)
        // Weak anchor (Horror) + exploratory (Comedy)
        val weakAnchor = CandidateAnimeItem(20L, listOf("Horror", "Comedy"), 8.0, 50000)

        val serendipityConfig = RecommendationPreset.SERENDIPITY.toConfig()
        val results =
            scorer.evaluateLab(
                userList = userList,
                candidates = listOf(weakAnchor, strongAnchor),
                config = serendipityConfig,
                targetType = TagType.GENRE,
            )

        assertEquals(1, results.size)
        assertEquals(10L, results.first().animeId)
    }
}
