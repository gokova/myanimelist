package com.gokova.myanimelist.feature.recommendation.domain.algorithm

import com.gokova.myanimelist.feature.recommendation.domain.model.CandidateAnimeItem
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

        // Ubiquitous Action candidates vs single School candidate
        val actionCandidates =
            (1L..10L).map { id ->
                CandidateAnimeItem(id, listOf("Action"), 7.5, 100000)
            }
        val schoolCandidate = CandidateAnimeItem(99L, listOf("School"), 8.5, 50000)
        val candidates = actionCandidates + schoolCandidate

        val result = scorer.evaluate(userList, candidates)

        val schoolResult = result.recommendations.first { it.animeId == 99L }
        // School is a Theme in AnimeTaxonomy, so its theme rank should be 1
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
}
