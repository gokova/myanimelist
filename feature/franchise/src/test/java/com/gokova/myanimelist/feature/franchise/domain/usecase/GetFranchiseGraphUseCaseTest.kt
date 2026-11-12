package com.gokova.myanimelist.feature.franchise.domain.usecase

import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.AnimeRelationshipEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseEdgeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetFranchiseGraphUseCaseTest {
    private lateinit var useCase: GetFranchiseGraphUseCase

    @Before
    fun setUp() {
        useCase = GetFranchiseGraphUseCase()
    }

    @Test
    fun `buildGraph normalizes prequel relations into chronological sequel edges`() {
        // Season 2 has prequel Season 1
        val rel =
            AnimeRelationshipEntity(
                sourceAnimeId = 2L,
                targetAnimeId = 1L,
                relationType = "prequel",
                relationTypeFormatted = "Prequel",
            )
        val animes =
            listOf(
                createAnimeEntity(1L, "Season 1", 2020),
                createAnimeEntity(2L, "Season 2", 2022),
            )

        val graph =
            useCase.buildGraph(
                rootAnimeId = 2L,
                relationships = listOf(rel),
                animeEntities = animes,
                userAnimeItems = emptyList(),
                ignoredAnimeIds = emptySet(),
            )

        assertEquals(2, graph.nodes.size)
        // Normalized edge points from Season 1 -> Season 2
        assertEquals(1, graph.edges.size)
        val edge = graph.edges.first()
        assertEquals(1L, edge.sourceId)
        assertEquals(2L, edge.targetId)
        assertEquals(FranchiseEdgeType.SEQUEL, edge.edgeType)

        // Mainline sequence is ordered [Season 1, Season 2]
        assertEquals(2, graph.mainlineSequence.size)
        assertEquals(1L, graph.mainlineSequence[0].animeId)
        assertEquals(2L, graph.mainlineSequence[1].animeId)
    }

    @Test
    fun `buildGraph deduplicates reciprocal sequel and prequel relations`() {
        val rel1 =
            AnimeRelationshipEntity(
                sourceAnimeId = 1L,
                targetAnimeId = 2L,
                relationType = "sequel",
                relationTypeFormatted = "Sequel",
            )
        val rel2 =
            AnimeRelationshipEntity(
                sourceAnimeId = 2L,
                targetAnimeId = 1L,
                relationType = "prequel",
                relationTypeFormatted = "Prequel",
            )
        val animes =
            listOf(
                createAnimeEntity(1L, "Season 1", 2020),
                createAnimeEntity(2L, "Season 2", 2022),
            )

        val graph =
            useCase.buildGraph(
                rootAnimeId = 1L,
                relationships = listOf(rel1, rel2),
                animeEntities = animes,
                userAnimeItems = emptyList(),
                ignoredAnimeIds = emptySet(),
            )

        assertEquals(1, graph.edges.size)
        assertEquals(1L, graph.edges.first().sourceId)
        assertEquals(2L, graph.edges.first().targetId)
    }

    @Test
    fun `buildGraph binds user anime list status and ignored flags`() {
        val animeEntity = createAnimeEntity(1L, "Solo Anime", 2023)
        val userItem =
            UserAnimeListItem(
                userAnime =
                    UserAnimeListEntity(
                        animeId = 1L,
                        status = "watching",
                        score = 8,
                        numEpisodesWatched = 5,
                        updatedAt = "2026-10-10",
                        isRewatching = false,
                    ),
                anime = animeEntity,
            )

        val graph =
            useCase.buildGraph(
                rootAnimeId = 1L,
                relationships = emptyList(),
                animeEntities = listOf(animeEntity),
                userAnimeItems = listOf(userItem),
                ignoredAnimeIds = setOf(2L),
            )

        val node = graph.nodes[1L]
        assertTrue(node?.isInUserList == true)
        assertEquals("watching", node?.userStatus)
        assertEquals(5, node?.episodesWatched)
        assertTrue(node?.isOrigin == true)
        assertFalse(node?.isNotInterested == true)
    }

    @Test
    fun `buildGraph groups side works by parent anime`() {
        val rel =
            AnimeRelationshipEntity(
                sourceAnimeId = 1L,
                targetAnimeId = 10L,
                relationType = "side_story",
                relationTypeFormatted = "Side Story",
            )
        val animes =
            listOf(
                createAnimeEntity(1L, "Season 1", 2020),
                createAnimeEntity(10L, "OVA", 2021),
            )

        val graph =
            useCase.buildGraph(
                rootAnimeId = 1L,
                relationships = listOf(rel),
                animeEntities = animes,
                userAnimeItems = emptyList(),
                ignoredAnimeIds = emptySet(),
            )

        assertEquals(1, graph.sideWorksByParent[1L]?.size)
        assertEquals(10L, graph.sideWorksByParent[1L]?.first()?.animeId)
    }

    @Test
    fun `buildGraph preserves root anime as mainline when root has no sequels`() {
        val rels =
            listOf(
                AnimeRelationshipEntity(21L, 100L, "side_story", "Side story"),
                AnimeRelationshipEntity(1L, 2L, "sequel", "Sequel"),
                AnimeRelationshipEntity(1L, 100L, "side_story", "Side story"),
            )
        val animes =
            listOf(
                createAnimeEntity(21L, "One Piece", 1999),
                createAnimeEntity(100L, "Crossover Special", 2003),
                createAnimeEntity(1L, "DBZ", 1989),
                createAnimeEntity(2L, "DBGT", 1996),
            )

        val graph =
            useCase.buildGraph(
                rootAnimeId = 21L,
                relationships = rels,
                animeEntities = animes,
                userAnimeItems = emptyList(),
                ignoredAnimeIds = emptySet(),
            )

        assertEquals(1, graph.mainlineSequence.size)
        assertEquals(21L, graph.mainlineSequence.first().animeId)
        assertEquals(setOf(21L, 100L), graph.nodes.keys)
        assertFalse(graph.sideWorksByParent.containsKey(1L))
    }

    @Test
    fun `buildGraph blocks foreign franchise traversal from crossover works`() {
        val rels =
            listOf(
                AnimeRelationshipEntity(21L, 100L, "side_story", "Side story"),
                AnimeRelationshipEntity(100L, 1L, "parent_story", "Parent story"),
                AnimeRelationshipEntity(1L, 2L, "sequel", "Sequel"),
                AnimeRelationshipEntity(1L, 3L, "side_story", "DB Movie"),
            )
        val animes =
            listOf(
                createAnimeEntity(21L, "One Piece", 1999),
                createAnimeEntity(100L, "Crossover Special", 2003),
                createAnimeEntity(1L, "DBZ", 1989),
                createAnimeEntity(2L, "DBGT", 1996),
                createAnimeEntity(3L, "DBZ Movie", 1993),
            )

        val graph =
            useCase.buildGraph(
                rootAnimeId = 21L,
                relationships = rels,
                animeEntities = animes,
                userAnimeItems = emptyList(),
                ignoredAnimeIds = emptySet(),
            )

        assertEquals(1, graph.mainlineSequence.size)
        assertEquals(21L, graph.mainlineSequence.first().animeId)
        assertEquals(setOf(21L, 100L), graph.nodes.keys)
        assertEquals(1, graph.edges.size)
        assertEquals(21L, graph.edges.first().sourceId)
        assertEquals(100L, graph.edges.first().targetId)
        assertFalse(graph.sideWorksByParent.containsKey(1L))
    }

    @Test
    fun `buildGraph excludes side work nodes that have no anime entity metadata`() {
        val rels =
            listOf(
                AnimeRelationshipEntity(21L, 200L, "side_story", "Side story"),
            )
        val animes =
            listOf(
                createAnimeEntity(21L, "One Piece", 1999),
            )

        val graph =
            useCase.buildGraph(
                rootAnimeId = 21L,
                relationships = rels,
                animeEntities = animes,
                userAnimeItems = emptyList(),
                ignoredAnimeIds = emptySet(),
            )

        assertEquals(setOf(21L), graph.nodes.keys)
        assertTrue(graph.sideWorksByParent[21L].isNullOrEmpty())
    }

    @Test
    fun `buildGraph selects parent franchise mainline when opened from spin-off series`() {
        val rels =
            listOf(
                AnimeRelationshipEntity(1L, 2L, "sequel", "Sequel"),
                AnimeRelationshipEntity(2L, 3L, "sequel", "Sequel"),
                AnimeRelationshipEntity(1L, 501L, "spin_off", "Spin-off"),
                AnimeRelationshipEntity(501L, 502L, "sequel", "Sequel"),
                AnimeRelationshipEntity(1L, 61269L, "other", "Other"),
            )
        val animes =
            listOf(
                createAnimeEntity(1L, "Digimon Adventure", 1999),
                createAnimeEntity(2L, "Digimon Adventure 02", 2000),
                createAnimeEntity(3L, "Digimon Adventure tri.", 2015),
                createAnimeEntity(501L, "Digimon Tamers", 2001),
                createAnimeEntity(502L, "Digimon Tamers: Runaway Locomon", 2002),
                createAnimeEntity(61269L, "Digimon Beatbreak", 2025),
            )

        val graph =
            useCase.buildGraph(
                rootAnimeId = 501L,
                relationships = rels,
                animeEntities = animes,
                userAnimeItems = emptyList(),
                ignoredAnimeIds = emptySet(),
            )

        // Mainline is the franchise spine: Adventure -> Adventure 02 -> tri
        assertEquals(3, graph.mainlineSequence.size)
        assertEquals(1L, graph.mainlineSequence[0].animeId)
        assertEquals(2L, graph.mainlineSequence[1].animeId)
        assertEquals(3L, graph.mainlineSequence[2].animeId)

        // Tamer is marked as the origin (current) node
        assertTrue(graph.nodes[501L]?.isOrigin == true)
        assertFalse(graph.nodes[1L]?.isOrigin == true)

        // Side works of Adventure include Tamers, its sequel Locomon, and Beatbreak
        val sideWorks =
            graph.sideWorksByParent[1L]
                ?.map { it.animeId }
                ?.toSet()
                .orEmpty()
        assertTrue(sideWorks.contains(501L))
        assertTrue(sideWorks.contains(502L))
        assertTrue(sideWorks.contains(61269L))
        assertEquals(setOf(1L, 2L, 3L, 501L, 502L, 61269L), graph.nodes.keys)
    }

    @Test
    fun `buildGraph ignores character cameos and does not link foreign franchise`() {
        val rels =
            listOf(
                AnimeRelationshipEntity(21L, 100L, "character", "Character"),
                AnimeRelationshipEntity(100L, 1L, "character", "Character"),
                AnimeRelationshipEntity(1L, 2L, "sequel", "Sequel"),
            )
        val animes =
            listOf(
                createAnimeEntity(21L, "One Piece", 1999),
                createAnimeEntity(100L, "Kyutai Panic Adventure!", 2003),
                createAnimeEntity(1L, "Dragon Ball Z", 1989),
                createAnimeEntity(2L, "Dragon Ball GT", 1996),
            )

        val graph =
            useCase.buildGraph(
                rootAnimeId = 21L,
                relationships = rels,
                animeEntities = animes,
                userAnimeItems = emptyList(),
                ignoredAnimeIds = emptySet(),
            )

        assertEquals(1, graph.mainlineSequence.size)
        assertEquals(21L, graph.mainlineSequence.first().animeId)
        // Only 21L is present since 100L was linked only via character
        assertEquals(setOf(21L), graph.nodes.keys)
        assertFalse(graph.nodes.containsKey(1L))
        assertFalse(graph.nodes.containsKey(2L))
    }

    @Test
    fun `buildGraph selects TV mainline over spin-off and summary chains for Attack on Titan`() {
        val rels =
            listOf(
                // TV series mainline
                AnimeRelationshipEntity(101L, 102L, "sequel", "Sequel"),
                AnimeRelationshipEntity(102L, 103L, "sequel", "Sequel"),
                // Chibi theater spin-off chain
                AnimeRelationshipEntity(101L, 201L, "spin_off", "Spin-off"),
                AnimeRelationshipEntity(201L, 202L, "sequel", "Sequel"),
                // Movie recap summary chain
                AnimeRelationshipEntity(101L, 301L, "summary", "Summary"),
                AnimeRelationshipEntity(301L, 302L, "sequel", "Sequel"),
                AnimeRelationshipEntity(302L, 303L, "sequel", "Sequel"),
                AnimeRelationshipEntity(102L, 303L, "summary", "Summary"),
            )
        val animes =
            listOf(
                createAnimeEntity(101L, "Attack on Titan Season 1", 2013),
                createAnimeEntity(102L, "Attack on Titan Season 2", 2017),
                createAnimeEntity(103L, "Attack on Titan Season 3", 2018),
                createAnimeEntity(201L, "AOT: Chibi Theater 1", 2013),
                createAnimeEntity(202L, "AOT: Chibi Theater 2", 2014),
                createAnimeEntity(301L, "AOT Movie: Crimson Bow", 2014),
                createAnimeEntity(302L, "AOT Movie: Wings of Freedom", 2015),
                createAnimeEntity(303L, "AOT Movie: Roar of Awakening", 2018),
            )

        // Test opening from Season 1
        val graphFromS1 =
            useCase.buildGraph(
                rootAnimeId = 101L,
                relationships = rels,
                animeEntities = animes,
                userAnimeItems = emptyList(),
                ignoredAnimeIds = emptySet(),
            )
        assertEquals(3, graphFromS1.mainlineSequence.size)
        assertEquals(101L, graphFromS1.mainlineSequence[0].animeId)
        assertEquals(102L, graphFromS1.mainlineSequence[1].animeId)
        assertEquals(103L, graphFromS1.mainlineSequence[2].animeId)

        // Test opening from Season 2
        val graphFromS2 =
            useCase.buildGraph(
                rootAnimeId = 102L,
                relationships = rels,
                animeEntities = animes,
                userAnimeItems = emptyList(),
                ignoredAnimeIds = emptySet(),
            )
        assertEquals(3, graphFromS2.mainlineSequence.size)
        assertEquals(101L, graphFromS2.mainlineSequence[0].animeId)
        assertEquals(102L, graphFromS2.mainlineSequence[1].animeId)
        assertEquals(103L, graphFromS2.mainlineSequence[2].animeId)
    }

    @Test
    fun `buildGraph preserves branching sequel paths as side paths without omitting nodes`() {
        val rels =
            listOf(
                AnimeRelationshipEntity(1L, 2L, "sequel", "Sequel"),
                AnimeRelationshipEntity(2L, 3L, "sequel", "Sequel"),
                AnimeRelationshipEntity(1L, 20L, "sequel", "Sequel"),
                AnimeRelationshipEntity(20L, 21L, "sequel", "Sequel"),
            )
        val animes =
            listOf(
                createAnimeEntity(1L, "Mainline 1", 2000),
                createAnimeEntity(2L, "Mainline 2", 2002),
                createAnimeEntity(3L, "Mainline 3", 2004),
                createAnimeEntity(20L, "Branch Sequel 1", 2005),
                createAnimeEntity(21L, "Branch Sequel 2", 2007),
            )

        val graph =
            useCase.buildGraph(
                rootAnimeId = 1L,
                relationships = rels,
                animeEntities = animes,
                userAnimeItems = emptyList(),
                ignoredAnimeIds = emptySet(),
            )

        assertEquals(3, graph.mainlineSequence.size)
        assertEquals(1L, graph.mainlineSequence[0].animeId)
        assertEquals(2L, graph.mainlineSequence[1].animeId)
        assertEquals(3L, graph.mainlineSequence[2].animeId)

        assertTrue(graph.nodes.containsKey(20L))
        assertTrue(graph.nodes.containsKey(21L))

        val sideWorksOf1 =
            graph.sideWorksByParent[1L]
                ?.map { it.animeId }
                ?.toSet()
                .orEmpty()
        assertTrue(sideWorksOf1.contains(20L))
        assertTrue(sideWorksOf1.contains(21L))

        val edge20To21 = graph.edges.find { it.sourceId == 20L && it.targetId == 21L }
        assertNotNull(edge20To21)
        assertEquals(FranchiseEdgeType.SEQUEL, edge20To21?.edgeType)
    }

    private fun createAnimeEntity(
        id: Long,
        title: String,
        year: Int,
    ) = AnimeEntity(
        id = id,
        title = title,
        titleEnglish = null,
        mainPictureMedium = null,
        mainPictureLarge = null,
        mediaType = "tv",
        airingStatus = "finished_airing",
        numEpisodes = 12,
        startSeasonYear = year,
        startSeasonSeason = "spring",
        meanScore = 8.5,
    )
}
