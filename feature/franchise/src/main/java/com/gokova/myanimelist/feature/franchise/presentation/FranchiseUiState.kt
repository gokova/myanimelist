package com.gokova.myanimelist.feature.franchise.presentation

import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraph
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraphLayout
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNode

enum class FranchiseViewMode {
    MAP,
    LIST,
}

data class FranchiseUiState(
    val rootAnimeId: Long = 0L,
    val isLoading: Boolean = true,
    val graph: FranchiseGraph? = null,
    val layout: FranchiseGraphLayout? = null,
    val selectedNode: FranchiseNode? = null,
    val viewMode: FranchiseViewMode = FranchiseViewMode.MAP,
    val isUpdatingEpisode: Boolean = false,
    val recenterTrigger: Int = 0,
    val fitTrigger: Int = 0,
    val zoomInTrigger: Int = 0,
    val zoomOutTrigger: Int = 0,
)
