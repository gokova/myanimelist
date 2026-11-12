package com.gokova.myanimelist.feature.franchise.domain.model

data class FranchiseGraph(
    val originAnimeId: Long,
    val nodes: Map<Long, FranchiseNode>,
    val edges: List<FranchiseEdge>,
    val mainlineSequence: List<FranchiseNode>,
    val sideWorksByParent: Map<Long, List<FranchiseNode>>,
)
