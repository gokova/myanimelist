package com.gokova.myanimelist.feature.franchise.domain.usecase

import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseEdge
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseEdgeType
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraph
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FranchiseLayoutCalculatorTest {
    private lateinit var calculator: FranchiseLayoutCalculator

    @Before
    fun setUp() {
        calculator = FranchiseLayoutCalculator()
    }

    @Test
    fun `empty graph produces empty layout`() {
        val graph =
            FranchiseGraph(
                originAnimeId = 1L,
                nodes = emptyMap(),
                edges = emptyList(),
                mainlineSequence = emptyList(),
                sideWorksByParent = emptyMap(),
            )

        val layout = calculator.calculateLayout(graph)

        assertTrue(layout.nodeLayouts.isEmpty())
        assertTrue(layout.connections.isEmpty())
        assertEquals(0f, layout.contentWidthDp, 0.001f)
    }

    @Test
    fun `linear mainline creates vertical sequence with connecting curves`() {
        val node1 = createNode(1L, "Season 1", isOrigin = true)
        val node2 = createNode(2L, "Season 2")
        val node3 = createNode(3L, "Movie Sequel")

        val graph =
            FranchiseGraph(
                originAnimeId = 1L,
                nodes = listOf(node1, node2, node3).associateBy { it.animeId },
                edges =
                    listOf(
                        FranchiseEdge(1L, 2L, FranchiseEdgeType.SEQUEL, "Sequel"),
                        FranchiseEdge(2L, 3L, FranchiseEdgeType.SEQUEL, "Sequel"),
                    ),
                mainlineSequence = listOf(node1, node2, node3),
                sideWorksByParent = emptyMap(),
            )

        val layout = calculator.calculateLayout(graph)

        assertEquals(3, layout.nodeLayouts.size)
        assertEquals(2, layout.connections.size)

        val l1 = layout.nodeLayouts.first { it.node.animeId == 1L }
        val l2 = layout.nodeLayouts.first { it.node.animeId == 2L }
        val l3 = layout.nodeLayouts.first { it.node.animeId == 3L }

        // Same X column (spine)
        assertEquals(l1.xDp, l2.xDp, 0.001f)
        assertEquals(l2.xDp, l3.xDp, 0.001f)

        // Monotonically increasing Y
        assertTrue(l1.yDp < l2.yDp)
        assertTrue(l2.yDp < l3.yDp)

        // Origin node is correctly identified
        assertNotNull(layout.originNodeLayout)
        assertEquals(1L, layout.originNodeLayout?.node?.animeId)
    }

    @Test
    fun `side works are positioned laterally to the mainline node`() {
        val node1 = createNode(1L, "Season 1", isOrigin = true)
        val ova = createNode(10L, "Season 1 OVA")
        val special = createNode(11L, "Season 1 Special")

        val graph =
            FranchiseGraph(
                originAnimeId = 1L,
                nodes = listOf(node1, ova, special).associateBy { it.animeId },
                edges =
                    listOf(
                        FranchiseEdge(1L, 10L, FranchiseEdgeType.SIDE_STORY, "Side story"),
                        FranchiseEdge(1L, 11L, FranchiseEdgeType.SIDE_STORY, "Special"),
                    ),
                mainlineSequence = listOf(node1),
                sideWorksByParent = mapOf(1L to listOf(ova, special)),
            )

        val layout = calculator.calculateLayout(graph)

        assertEquals(3, layout.nodeLayouts.size)
        val mainLayout = layout.nodeLayouts.first { it.node.animeId == 1L }
        val ovaLayout = layout.nodeLayouts.first { it.node.animeId == 10L }
        val specialLayout = layout.nodeLayouts.first { it.node.animeId == 11L }

        // Left side work is to the left of spine
        assertTrue(ovaLayout.xDp < mainLayout.xDp)
        // Right side work is to the right of spine
        assertTrue(specialLayout.xDp > mainLayout.xDp)
    }

    @Test
    fun `side works with sequel relations are clustered together and connected with curves`() {
        val main = createNode(1L, "Main Season", isOrigin = true)
        val branch1 = createNode(10L, "Movie Part 1")
        val branch2 = createNode(11L, "Movie Part 2")

        val graph =
            FranchiseGraph(
                originAnimeId = 1L,
                nodes = listOf(main, branch1, branch2).associateBy { it.animeId },
                edges =
                    listOf(
                        FranchiseEdge(1L, 10L, FranchiseEdgeType.SPIN_OFF, "Spin-off"),
                        FranchiseEdge(10L, 11L, FranchiseEdgeType.SEQUEL, "Sequel"),
                    ),
                mainlineSequence = listOf(main),
                sideWorksByParent = mapOf(1L to listOf(branch1, branch2)),
            )

        val layout = calculator.calculateLayout(graph)

        assertEquals(3, layout.nodeLayouts.size)
        val l10 = layout.nodeLayouts.first { it.node.animeId == 10L }
        val l11 = layout.nodeLayouts.first { it.node.animeId == 11L }

        // Both branch nodes are in the same side column
        assertEquals(l10.xDp, l11.xDp, 0.001f)
        assertTrue(l10.yDp < l11.yDp)

        // Connection exists between branch1 and branch2
        assertEquals(1, layout.connections.size)
        val conn = layout.connections.first()
        assertEquals(10L, conn.fromNodeId)
        assertEquals(11L, conn.toNodeId)
    }

    private fun createNode(
        id: Long,
        title: String,
        isOrigin: Boolean = false,
    ) = FranchiseNode(
        animeId = id,
        title = title,
        titleEnglish = null,
        posterMedium = null,
        posterLarge = null,
        mediaType = "tv",
        numEpisodes = 12,
        isInUserList = true,
        userStatus = "completed",
        episodesWatched = 12,
        isRewatching = false,
        isNotInterested = false,
        isOrigin = isOrigin,
    )
}
