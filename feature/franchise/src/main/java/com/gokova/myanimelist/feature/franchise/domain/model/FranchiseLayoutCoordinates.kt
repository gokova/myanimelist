package com.gokova.myanimelist.feature.franchise.domain.model

data class FranchiseNodeLayout(
    val node: FranchiseNode,
    val xDp: Float,
    val yDp: Float,
    val widthDp: Float,
    val heightDp: Float,
)

data class FranchiseConnectionCurve(
    val fromNodeId: Long,
    val toNodeId: Long,
    val startXDp: Float,
    val startYDp: Float,
    val endXDp: Float,
    val endYDp: Float,
    val label: String,
)

data class FranchiseGraphLayout(
    val nodeLayouts: List<FranchiseNodeLayout>,
    val connections: List<FranchiseConnectionCurve>,
    val contentWidthDp: Float,
    val contentHeightDp: Float,
    val originNodeLayout: FranchiseNodeLayout?,
)
