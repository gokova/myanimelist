package com.gokova.myanimelist.feature.franchise.presentation

import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNode

data class FranchiseActions(
    val onBackClick: () -> Unit = {},
    val onToggleViewMode: () -> Unit = {},
    val onNodeClick: (FranchiseNode) -> Unit = {},
    val onDismissBottomSheet: () -> Unit = {},
    val onOpenDetails: (Long) -> Unit = {},
    val onIncrementEpisode: (Long) -> Unit = {},
    val onUnignoreAnime: (Long) -> Unit = {},
    val onRecenterClick: () -> Unit = {},
    val onFitClick: () -> Unit = {},
    val onZoomInClick: () -> Unit = {},
    val onZoomOutClick: () -> Unit = {},
)
