package com.gokova.myanimelist.feature.franchise.domain.usecase

import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseConnectionCurve
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseEdge
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseEdgeType
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraph
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraphLayout
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNode
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNodeLayout
import javax.inject.Inject
import kotlin.math.max

@Suppress("TooManyFunctions")
class FranchiseLayoutCalculator
    @Inject
    constructor() {
        companion object {
            const val CARD_WIDTH_DP = 240f
            const val CARD_HEIGHT_DP = 88f
            const val GAP_X_DP = 32f
            const val GAP_Y_DP = 48f
            const val SIDE_ITEM_GAP_Y_DP = 12f
            const val MARGIN_DP = 48f
        }

        fun calculateLayout(graph: FranchiseGraph): FranchiseGraphLayout {
            if (graph.nodes.isEmpty()) {
                return FranchiseGraphLayout(
                    nodeLayouts = emptyList(),
                    connections = emptyList(),
                    contentWidthDp = 0f,
                    contentHeightDp = 0f,
                    originNodeLayout = null,
                )
            }

            val rawLayouts = mutableListOf<FranchiseNodeLayout>()
            val connections = mutableListOf<FranchiseConnectionCurve>()
            val spineX = CARD_WIDTH_DP + GAP_X_DP
            var currentY = 0f

            val mainline =
                graph.mainlineSequence.ifEmpty {
                    listOfNotNull(
                        graph.nodes[graph.originAnimeId] ?: graph.nodes.values.firstOrNull(),
                    )
                }

            val placedNodeIds = mutableSetOf<Long>()
            val context =
                MainlineLayoutContext(
                    graph = graph,
                    spineX = spineX,
                    placedNodeIds = placedNodeIds,
                    rawLayouts = rawLayouts,
                    connections = connections,
                )

            for (index in mainline.indices) {
                currentY =
                    layoutMainlineRow(
                        mainNode = mainline[index],
                        index = index,
                        mainline = mainline,
                        currentY = currentY,
                        context = context,
                    )
            }

            layoutRemainingNodes(graph, placedNodeIds, spineX, currentY, rawLayouts)
            return buildFinalLayout(rawLayouts, connections, graph.originAnimeId)
        }

        private fun layoutMainlineRow(
            mainNode: FranchiseNode,
            index: Int,
            mainline: List<FranchiseNode>,
            currentY: Float,
            context: MainlineLayoutContext,
        ): Float {
            context.placedNodeIds.add(mainNode.animeId)

            val sideWorks =
                context.graph.sideWorksByParent[mainNode.animeId]
                    .orEmpty()
                    .filter { it.animeId !in context.placedNodeIds }

            val leftSideWorks = mutableListOf<FranchiseNode>()
            val rightSideWorks = mutableListOf<FranchiseNode>()

            val chains = groupSideWorksIntoChains(sideWorks, context.graph.edges)
            distributeSideWorkChains(
                chains = chains,
                leftSideWorks = leftSideWorks,
                rightSideWorks = rightSideWorks,
                placedNodeIds = context.placedNodeIds,
            )

            val leftHeight = calculateSideClusterHeight(leftSideWorks.size)
            val rightHeight = calculateSideClusterHeight(rightSideWorks.size)
            val rowHeight = max(CARD_HEIGHT_DP, max(leftHeight, rightHeight))

            val mainNodeY = currentY + (rowHeight - CARD_HEIGHT_DP) / 2f
            val mainNodeLayout =
                FranchiseNodeLayout(
                    node = mainNode,
                    xDp = context.spineX,
                    yDp = mainNodeY,
                    widthDp = CARD_WIDTH_DP,
                    heightDp = CARD_HEIGHT_DP,
                )
            context.rawLayouts.add(mainNodeLayout)

            val leftX = context.spineX - CARD_WIDTH_DP - GAP_X_DP
            val rightX = context.spineX + CARD_WIDTH_DP + GAP_X_DP
            layoutSideWorksList(leftSideWorks, leftX, currentY, context)
            layoutSideWorksList(rightSideWorks, rightX, currentY, context)

            if (index > 0) {
                val prevMainNode =
                    context.rawLayouts.first {
                        it.node.animeId == mainline[index - 1].animeId
                    }
                context.connections.add(
                    FranchiseConnectionCurve(
                        fromNodeId = prevMainNode.node.animeId,
                        toNodeId = mainNode.animeId,
                        startXDp = prevMainNode.xDp + CARD_WIDTH_DP / 2f,
                        startYDp = prevMainNode.yDp + CARD_HEIGHT_DP,
                        endXDp = mainNodeLayout.xDp + CARD_WIDTH_DP / 2f,
                        endYDp = mainNodeLayout.yDp,
                        label = "Sequel",
                    ),
                )
            }

            return currentY + rowHeight + GAP_Y_DP
        }

        private fun groupSideWorksIntoChains(
            sideWorks: List<FranchiseNode>,
            edges: List<FranchiseEdge>,
        ): List<List<FranchiseNode>> {
            if (sideWorks.isEmpty()) return emptyList()

            val sideWorkIds = sideWorks.map { it.animeId }.toSet()
            val nodesById = sideWorks.associateBy { it.animeId }

            val sequelEdges =
                edges.filter {
                    it.edgeType == FranchiseEdgeType.SEQUEL &&
                        it.sourceId in sideWorkIds &&
                        it.targetId in sideWorkIds
                }

            val childrenMap = sequelEdges.groupBy({ it.sourceId }, { it.targetId })
            val targetIds = sequelEdges.map { it.targetId }.toSet()

            val roots = sideWorks.filter { it.animeId !in targetIds }
            val chains = mutableListOf<List<FranchiseNode>>()
            val visited = mutableSetOf<Long>()

            for (root in roots) {
                if (root.animeId in visited) continue
                val chain = mutableListOf<FranchiseNode>()
                var curr: Long? = root.animeId
                while (curr != null && visited.add(curr)) {
                    nodesById[curr]?.let { chain.add(it) }
                    curr = childrenMap[curr]?.firstOrNull { it !in visited }
                }
                if (chain.isNotEmpty()) {
                    chains.add(chain)
                }
            }

            for (sw in sideWorks) {
                if (visited.add(sw.animeId)) {
                    chains.add(listOf(sw))
                }
            }

            return chains.sortedByDescending { it.size }
        }

        private fun distributeSideWorkChains(
            chains: List<List<FranchiseNode>>,
            leftSideWorks: MutableList<FranchiseNode>,
            rightSideWorks: MutableList<FranchiseNode>,
            placedNodeIds: MutableSet<Long>,
        ) {
            var leftCount = 0
            var rightCount = 0

            for (chain in chains) {
                if (leftCount <= rightCount) {
                    leftSideWorks.addAll(chain)
                    leftCount += chain.size
                } else {
                    rightSideWorks.addAll(chain)
                    rightCount += chain.size
                }
                for (node in chain) {
                    placedNodeIds.add(node.animeId)
                }
            }
        }

        private fun calculateSideClusterHeight(count: Int): Float =
            if (count > 0) {
                count * CARD_HEIGHT_DP + (count - 1) * SIDE_ITEM_GAP_Y_DP
            } else {
                0f
            }

        private fun layoutSideWorksList(
            sideWorks: List<FranchiseNode>,
            xDp: Float,
            startY: Float,
            context: MainlineLayoutContext,
        ) {
            var y = startY
            val layouts = mutableMapOf<Long, FranchiseNodeLayout>()
            for (side in sideWorks) {
                val layout =
                    FranchiseNodeLayout(
                        node = side,
                        xDp = xDp,
                        yDp = y,
                        widthDp = CARD_WIDTH_DP,
                        heightDp = CARD_HEIGHT_DP,
                    )
                layouts[side.animeId] = layout
                context.rawLayouts.add(layout)
                y += CARD_HEIGHT_DP + SIDE_ITEM_GAP_Y_DP
            }

            val sideWorkIds = sideWorks.map { it.animeId }.toSet()
            for (edge in context.graph.edges) {
                val isSequelInColumn =
                    edge.edgeType == FranchiseEdgeType.SEQUEL &&
                        edge.sourceId in sideWorkIds &&
                        edge.targetId in sideWorkIds
                if (isSequelInColumn) {
                    val fromLayout = layouts[edge.sourceId]
                    val toLayout = layouts[edge.targetId]
                    if (fromLayout != null && toLayout != null && fromLayout.yDp < toLayout.yDp) {
                        context.connections.add(
                            FranchiseConnectionCurve(
                                fromNodeId = edge.sourceId,
                                toNodeId = edge.targetId,
                                startXDp = fromLayout.xDp + CARD_WIDTH_DP / 2f,
                                startYDp = fromLayout.yDp + CARD_HEIGHT_DP,
                                endXDp = toLayout.xDp + CARD_WIDTH_DP / 2f,
                                endYDp = toLayout.yDp,
                                label = "Sequel",
                            ),
                        )
                    }
                }
            }
        }

        private fun layoutRemainingNodes(
            graph: FranchiseGraph,
            placedNodeIds: Set<Long>,
            spineX: Float,
            startY: Float,
            rawLayouts: MutableList<FranchiseNodeLayout>,
        ) {
            var y = startY
            val remainingNodes = graph.nodes.values.filter { it.animeId !in placedNodeIds }
            for (rem in remainingNodes) {
                rawLayouts.add(
                    FranchiseNodeLayout(
                        node = rem,
                        xDp = spineX,
                        yDp = y,
                        widthDp = CARD_WIDTH_DP,
                        heightDp = CARD_HEIGHT_DP,
                    ),
                )
                y += CARD_HEIGHT_DP + GAP_Y_DP
            }
        }

        private fun buildFinalLayout(
            rawLayouts: List<FranchiseNodeLayout>,
            connections: List<FranchiseConnectionCurve>,
            originAnimeId: Long,
        ): FranchiseGraphLayout {
            val minX = rawLayouts.minOf { it.xDp }
            val maxX = rawLayouts.maxOf { it.xDp + it.widthDp }
            val minY = rawLayouts.minOf { it.yDp }
            val maxY = rawLayouts.maxOf { it.yDp + it.heightDp }

            val shiftX = -minX + MARGIN_DP
            val shiftY = -minY + MARGIN_DP

            val adjustedLayouts =
                rawLayouts.map { layout ->
                    layout.copy(
                        xDp = layout.xDp + shiftX,
                        yDp = layout.yDp + shiftY,
                    )
                }

            val adjustedConnections =
                connections.map { conn ->
                    conn.copy(
                        startXDp = conn.startXDp + shiftX,
                        startYDp = conn.startYDp + shiftY,
                        endXDp = conn.endXDp + shiftX,
                        endYDp = conn.endYDp + shiftY,
                    )
                }

            val totalWidthDp = maxX - minX + 2 * MARGIN_DP
            val totalHeightDp = maxY - minY + 2 * MARGIN_DP
            val originLayout = adjustedLayouts.find { it.node.animeId == originAnimeId }

            return FranchiseGraphLayout(
                nodeLayouts = adjustedLayouts,
                connections = adjustedConnections,
                contentWidthDp = totalWidthDp,
                contentHeightDp = totalHeightDp,
                originNodeLayout = originLayout,
            )
        }

        private data class MainlineLayoutContext(
            val graph: FranchiseGraph,
            val spineX: Float,
            val placedNodeIds: MutableSet<Long>,
            val rawLayouts: MutableList<FranchiseNodeLayout>,
            val connections: MutableList<FranchiseConnectionCurve>,
        )
    }
