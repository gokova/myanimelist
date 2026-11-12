package com.gokova.myanimelist.feature.franchise.domain.usecase

import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.AnimeRelationshipEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseEdge
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseEdgeType
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraph
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNode
import javax.inject.Inject

private const val MAX_CANDIDATE_CHAINS = 20

private val NON_EXPANDABLE_SIDE_RELATIONS =
    setOf("sequel", "prequel", "character", "parent_story")

private val INCOMING_PARENT_RELATIONS =
    setOf(
        "spin_off",
        "side_story",
        "summary",
        "alternative_setting",
        "alternative_version",
        "other",
    )

private class SideWorkExpansionContext(
    val relationships: List<AnimeRelationshipEntity>,
    val sequelChildren: Map<Long, List<Long>>,
    val allNodes: Map<Long, FranchiseNode>,
    val sideWorksByParent: MutableMap<Long, MutableList<FranchiseNode>>,
    val visited: MutableSet<Long>,
    val queue: ArrayDeque<Pair<Long, Long>>,
)

private class SideWorkProcessingContext(
    val mainlineIdSet: Set<Long>,
    val allNodes: Map<Long, FranchiseNode>,
    val deduplicatedEdges: MutableMap<Pair<Long, Long>, FranchiseEdge>,
    val sideWorksByParent: MutableMap<Long, MutableList<FranchiseNode>>,
)

@Suppress("TooManyFunctions")
class GetFranchiseGraphUseCase
    @Inject
    constructor() {
        @Suppress("LongMethod")
        fun buildGraph(
            rootAnimeId: Long,
            relationships: List<AnimeRelationshipEntity>,
            animeEntities: List<AnimeEntity>,
            userAnimeItems: List<UserAnimeListItem>,
            ignoredAnimeIds: Set<Long>,
        ): FranchiseGraph {
            val entitiesById = animeEntities.associateBy { it.id }
            val userItemsById = userAnimeItems.associateBy { it.anime.id }

            val allAnimeIds =
                (
                    relationships.map { it.sourceAnimeId } +
                        relationships.map { it.targetAnimeId } +
                        listOf(rootAnimeId)
                ).filter { id ->
                    id == rootAnimeId || entitiesById[id] != null
                }.toSet()

            val allNodes =
                allAnimeIds.associateWith { id ->
                    createNode(
                        id = id,
                        rootAnimeId = rootAnimeId,
                        entity = entitiesById[id],
                        userItem = userItemsById[id],
                        isIgnored = id in ignoredAnimeIds,
                    )
                }

            val deduplicatedEdges = mutableMapOf<Pair<Long, Long>, FranchiseEdge>()
            val sequelChildren = mutableMapOf<Long, MutableList<Long>>()
            val sequelParents = mutableMapOf<Long, MutableList<Long>>()

            processSequelEdges(
                relationships = relationships,
                deduplicatedEdges = deduplicatedEdges,
                sequelChildren = sequelChildren,
                sequelParents = sequelParents,
            )

            val mainlineIds =
                resolveMainlineIds(
                    rootAnimeId = rootAnimeId,
                    relationships = relationships,
                    sequelChildren = sequelChildren,
                    sequelParents = sequelParents,
                    entitiesById = entitiesById,
                )
            val mainlineIdSet = mainlineIds.toSet()

            val sideWorksByParent = mutableMapOf<Long, MutableList<FranchiseNode>>()
            processSideWorks(
                relationships = relationships,
                mainlineIdSet = mainlineIdSet,
                allNodes = allNodes,
                deduplicatedEdges = deduplicatedEdges,
                sideWorksByParent = sideWorksByParent,
            )

            linkSideWorkRelations(
                relationships = relationships,
                mainlineIdSet = mainlineIdSet,
                sequelChildren = sequelChildren,
                allNodes = allNodes,
                sideWorksByParent = sideWorksByParent,
            )

            val directSideWorkIds =
                sideWorksByParent.values
                    .flatten()
                    .map { it.animeId }
                    .toSet()
            val validFranchiseIds = mainlineIdSet + directSideWorkIds + setOf(rootAnimeId)

            val prunedNodes = allNodes.filterKeys { it in validFranchiseIds }
            val prunedEdges =
                deduplicatedEdges.values.filter {
                    it.sourceId in validFranchiseIds && it.targetId in validFranchiseIds
                }
            val prunedSideWorks = sideWorksByParent.filterKeys { it in mainlineIdSet }
            val mainlineNodes = mainlineIds.mapNotNull { prunedNodes[it] }

            return FranchiseGraph(
                originAnimeId = rootAnimeId,
                nodes = prunedNodes,
                edges = prunedEdges,
                mainlineSequence = mainlineNodes,
                sideWorksByParent = prunedSideWorks,
            )
        }

        private fun createNode(
            id: Long,
            rootAnimeId: Long,
            entity: AnimeEntity?,
            userItem: UserAnimeListItem?,
            isIgnored: Boolean,
        ): FranchiseNode =
            FranchiseNode(
                animeId = id,
                title = entity?.title ?: "Anime #$id",
                titleEnglish = entity?.titleEnglish,
                posterMedium = entity?.mainPictureMedium,
                posterLarge = entity?.mainPictureLarge,
                mediaType = entity?.mediaType,
                numEpisodes = entity?.numEpisodes,
                isInUserList = userItem != null,
                userStatus = userItem?.userAnime?.status,
                episodesWatched = userItem?.userAnime?.numEpisodesWatched ?: 0,
                isRewatching = userItem?.userAnime?.isRewatching ?: false,
                isNotInterested = isIgnored,
                isOrigin = id == rootAnimeId,
            )

        private fun processSequelEdges(
            relationships: List<AnimeRelationshipEntity>,
            deduplicatedEdges: MutableMap<Pair<Long, Long>, FranchiseEdge>,
            sequelChildren: MutableMap<Long, MutableList<Long>>,
            sequelParents: MutableMap<Long, MutableList<Long>>,
        ) {
            for (rel in relationships) {
                when (rel.relationType.lowercase()) {
                    "sequel" -> {
                        val key = Pair(rel.sourceAnimeId, rel.targetAnimeId)
                        deduplicatedEdges[key] =
                            FranchiseEdge(
                                sourceId = rel.sourceAnimeId,
                                targetId = rel.targetAnimeId,
                                edgeType = FranchiseEdgeType.SEQUEL,
                                label = rel.relationTypeFormatted,
                            )
                        sequelChildren
                            .getOrPut(rel.sourceAnimeId) { mutableListOf() }
                            .add(rel.targetAnimeId)
                        sequelParents
                            .getOrPut(rel.targetAnimeId) { mutableListOf() }
                            .add(rel.sourceAnimeId)
                    }
                    "prequel" -> {
                        val key = Pair(rel.targetAnimeId, rel.sourceAnimeId)
                        if (key !in deduplicatedEdges) {
                            deduplicatedEdges[key] =
                                FranchiseEdge(
                                    sourceId = rel.targetAnimeId,
                                    targetId = rel.sourceAnimeId,
                                    edgeType = FranchiseEdgeType.SEQUEL,
                                    label = "Sequel",
                                )
                            sequelChildren
                                .getOrPut(rel.targetAnimeId) { mutableListOf() }
                                .add(rel.sourceAnimeId)
                            sequelParents
                                .getOrPut(rel.sourceAnimeId) { mutableListOf() }
                                .add(rel.targetAnimeId)
                        }
                    }
                }
            }
        }

        private fun resolveMainlineIds(
            rootAnimeId: Long,
            relationships: List<AnimeRelationshipEntity>,
            sequelChildren: Map<Long, List<Long>>,
            sequelParents: Map<Long, List<Long>>,
            entitiesById: Map<Long, AnimeEntity>,
        ): List<Long> {
            val sequelNodes = (sequelChildren.keys + sequelParents.keys).toSet()
            if (sequelNodes.isEmpty()) return listOf(rootAnimeId)

            val candidateRoots = findCandidateRoots(sequelNodes, sequelParents, entitiesById)
            val chains =
                candidateRoots.flatMap { root ->
                    traceChainsFromRoot(root, sequelChildren, entitiesById)
                }

            return selectBestMainline(rootAnimeId, chains, relationships)
        }

        private fun findCandidateRoots(
            sequelNodes: Set<Long>,
            sequelParents: Map<Long, List<Long>>,
            entitiesById: Map<Long, AnimeEntity>,
        ): Set<Long> {
            val candidateRoots = mutableSetOf<Long>()
            for (node in sequelNodes) {
                val earliest = findEarliestMainlineAncestor(node, sequelParents, entitiesById)
                candidateRoots.add(earliest)
            }
            return candidateRoots
        }

        private fun selectBestMainline(
            rootAnimeId: Long,
            chains: List<List<Long>>,
            relationships: List<AnimeRelationshipEntity>,
        ): List<Long> {
            val rootChains = chains.filter { rootAnimeId in it }
            val rootChain = rootChains.maxByOrNull { it.size }
            val parentChain = findParentChain(rootAnimeId, chains, relationships)

            return when {
                rootChain != null && parentChain == null -> rootChain
                rootChain != null && parentChain != null -> {
                    if (parentChain.size > rootChain.size) parentChain else rootChain
                }
                else -> parentChain ?: listOf(rootAnimeId)
            }
        }

        private fun findParentChain(
            rootAnimeId: Long,
            chains: List<List<Long>>,
            relationships: List<AnimeRelationshipEntity>,
        ): List<Long>? {
            val parentIds =
                relationships
                    .filter {
                        val relType = it.relationType.lowercase()
                        val isOutgoingParent =
                            it.sourceAnimeId == rootAnimeId && relType == "parent_story"
                        val isIncomingParent =
                            it.targetAnimeId == rootAnimeId && relType in INCOMING_PARENT_RELATIONS
                        isOutgoingParent || isIncomingParent
                    }.map {
                        if (it.sourceAnimeId == rootAnimeId) it.targetAnimeId else it.sourceAnimeId
                    }.toSet()

            return chains
                .filter { chain ->
                    chain.none { it == rootAnimeId } && chain.any { it in parentIds }
                }.maxByOrNull { it.size }
        }

        private fun findEarliestMainlineAncestor(
            effectiveRoot: Long,
            sequelParents: Map<Long, List<Long>>,
            entitiesById: Map<Long, AnimeEntity>,
        ): Long {
            var earliest = effectiveRoot
            val visitedAncestors = mutableSetOf<Long>()
            while (true) {
                visitedAncestors.add(earliest)
                val parents = sequelParents[earliest]?.filter { it !in visitedAncestors }
                if (parents.isNullOrEmpty()) break
                earliest =
                    parents.minByOrNull { entitiesById[it]?.startSeasonYear ?: Int.MAX_VALUE }
                        ?: parents.first()
            }
            return earliest
        }

        private fun traceChainsFromRoot(
            root: Long,
            sequelChildren: Map<Long, List<Long>>,
            entitiesById: Map<Long, AnimeEntity>,
        ): List<List<Long>> {
            val chains = mutableListOf<List<Long>>()
            val queue = ArrayDeque<List<Long>>()
            queue.add(listOf(root))

            while (queue.isNotEmpty() && chains.size < MAX_CANDIDATE_CHAINS) {
                val path = queue.removeFirst()
                val current = path.last()
                val nextCandidates =
                    sequelChildren[current]
                        .orEmpty()
                        .filter { it !in path }
                        .sortedBy { entitiesById[it]?.startSeasonYear ?: Int.MAX_VALUE }

                if (nextCandidates.isEmpty()) {
                    chains.add(path)
                } else {
                    for (next in nextCandidates) {
                        queue.add(path + next)
                    }
                }
            }
            return chains
        }

        private fun processSideWorks(
            relationships: List<AnimeRelationshipEntity>,
            mainlineIdSet: Set<Long>,
            allNodes: Map<Long, FranchiseNode>,
            deduplicatedEdges: MutableMap<Pair<Long, Long>, FranchiseEdge>,
            sideWorksByParent: MutableMap<Long, MutableList<FranchiseNode>>,
        ) {
            val context =
                SideWorkProcessingContext(
                    mainlineIdSet = mainlineIdSet,
                    allNodes = allNodes,
                    deduplicatedEdges = deduplicatedEdges,
                    sideWorksByParent = sideWorksByParent,
                )
            for (rel in relationships) {
                if (shouldSkipSideWorkRelation(rel, context.mainlineIdSet)) continue
                processSingleSideWorkRelation(rel, context)
            }
        }

        private fun shouldSkipSideWorkRelation(
            rel: AnimeRelationshipEntity,
            mainlineIdSet: Set<Long>,
        ): Boolean {
            val relType = rel.relationType.lowercase()
            if (relType == "character") return true
            val isBothMainline =
                rel.sourceAnimeId in mainlineIdSet && rel.targetAnimeId in mainlineIdSet
            val isSequelRel = relType == "sequel" || relType == "prequel"
            return isSequelRel && isBothMainline
        }

        private fun processSingleSideWorkRelation(
            rel: AnimeRelationshipEntity,
            context: SideWorkProcessingContext,
        ) {
            if (rel.sourceAnimeId in context.mainlineIdSet &&
                rel.targetAnimeId !in context.mainlineIdSet
            ) {
                recordSideWork(
                    parentId = rel.sourceAnimeId,
                    childId = rel.targetAnimeId,
                    rel = rel,
                    context = context,
                )
            } else if (
                rel.targetAnimeId in context.mainlineIdSet &&
                rel.sourceAnimeId !in context.mainlineIdSet
            ) {
                recordSideWork(
                    parentId = rel.targetAnimeId,
                    childId = rel.sourceAnimeId,
                    rel = rel,
                    context = context,
                )
            }
        }

        private fun recordSideWork(
            parentId: Long,
            childId: Long,
            rel: AnimeRelationshipEntity,
            context: SideWorkProcessingContext,
        ) {
            val edgeType = FranchiseEdgeType.fromRelationType(rel.relationType)
            val key = Pair(parentId, childId)
            if (key !in context.deduplicatedEdges) {
                context.deduplicatedEdges[key] =
                    FranchiseEdge(
                        sourceId = parentId,
                        targetId = childId,
                        edgeType = edgeType,
                        label = rel.relationTypeFormatted,
                    )
            }
            addSideWorkNode(
                parentId = parentId,
                childId = childId,
                allNodes = context.allNodes,
                sideWorksByParent = context.sideWorksByParent,
            )
        }

        private fun linkSideWorkRelations(
            relationships: List<AnimeRelationshipEntity>,
            mainlineIdSet: Set<Long>,
            sequelChildren: Map<Long, List<Long>>,
            allNodes: Map<Long, FranchiseNode>,
            sideWorksByParent: MutableMap<Long, MutableList<FranchiseNode>>,
        ) {
            val directSideWorkIds =
                sideWorksByParent.values
                    .flatten()
                    .map { it.animeId }
                    .toSet()
            val visited = (mainlineIdSet + directSideWorkIds).toMutableSet()
            val queue = ArrayDeque<Pair<Long, Long>>()

            sideWorksByParent.forEach { (parentId, children) ->
                children.forEach { child -> queue.add(child.animeId to parentId) }
            }

            val context =
                SideWorkExpansionContext(
                    relationships = relationships,
                    sequelChildren = sequelChildren,
                    allNodes = allNodes,
                    sideWorksByParent = sideWorksByParent,
                    visited = visited,
                    queue = queue,
                )

            while (queue.isNotEmpty()) {
                val (currId, parentId) = queue.removeFirst()
                expandSideWorkSequels(currId, parentId, context)
                expandSideWorkNarratives(currId, parentId, context)
            }
        }

        private fun expandSideWorkSequels(
            currId: Long,
            parentId: Long,
            context: SideWorkExpansionContext,
        ) {
            val unvisitedChildren =
                context.sequelChildren[currId].orEmpty().filter { context.visited.add(it) }
            for (nextId in unvisitedChildren) {
                addSideWorkNode(parentId, nextId, context.allNodes, context.sideWorksByParent)
                context.queue.add(nextId to parentId)
            }
        }

        private fun expandSideWorkNarratives(
            currId: Long,
            parentId: Long,
            context: SideWorkExpansionContext,
        ) {
            for (rel in context.relationships) {
                val relType = rel.relationType.lowercase()
                if (relType in NON_EXPANDABLE_SIDE_RELATIONS) continue

                val isChild = rel.sourceAnimeId == currId && rel.targetAnimeId !in context.visited
                if (isChild && context.visited.add(rel.targetAnimeId)) {
                    addSideWorkNode(
                        parentId,
                        rel.targetAnimeId,
                        context.allNodes,
                        context.sideWorksByParent,
                    )
                    context.queue.add(rel.targetAnimeId to parentId)
                }
            }
        }

        private fun addSideWorkNode(
            parentId: Long,
            childId: Long,
            allNodes: Map<Long, FranchiseNode>,
            sideWorksByParent: MutableMap<Long, MutableList<FranchiseNode>>,
        ) {
            allNodes[childId]?.let { childNode ->
                val list = sideWorksByParent.getOrPut(parentId) { mutableListOf() }
                if (list.none { it.animeId == childId }) {
                    list.add(childNode)
                }
            }
        }
    }
