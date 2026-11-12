package com.gokova.myanimelist.feature.franchise.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gokova.myanimelist.feature.franchise.R
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNode
import com.gokova.myanimelist.feature.franchise.domain.repository.FranchiseRepository
import com.gokova.myanimelist.feature.franchise.domain.usecase.FranchiseLayoutCalculator
import com.gokova.myanimelist.feature.franchise.navigation.FranchiseRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class FranchiseViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: FranchiseRepository,
        private val layoutCalculator: FranchiseLayoutCalculator,
    ) : ViewModel() {
        val rootAnimeId: Long =
            runCatching { savedStateHandle.toRoute<FranchiseRoute>().animeId }
                .getOrNull() ?: (savedStateHandle.get<Long>("animeId") ?: 0L)

        private val _uiState = MutableStateFlow(FranchiseUiState(rootAnimeId = rootAnimeId))
        val uiState: StateFlow<FranchiseUiState> = _uiState.asStateFlow()

        private val eventChannel = Channel<FranchiseUiEvent>(Channel.BUFFERED)
        val uiEvents = eventChannel.receiveAsFlow()

        init {
            observeGraph()
        }

        private fun observeGraph() {
            viewModelScope.launch {
                repository.observeFranchiseGraph(rootAnimeId).collect { graph ->
                    val layout = layoutCalculator.calculateLayout(graph)
                    _uiState.update { current ->
                        val updatedSelected =
                            current.selectedNode?.let { selected ->
                                graph.nodes[selected.animeId]
                            }
                        current.copy(
                            isLoading = false,
                            graph = graph,
                            layout = layout,
                            selectedNode = updatedSelected,
                        )
                    }
                }
            }
        }

        fun onNodeClick(node: FranchiseNode) {
            _uiState.update { it.copy(selectedNode = node) }
        }

        fun onDismissBottomSheet() {
            _uiState.update { it.copy(selectedNode = null) }
        }

        fun onToggleViewMode() {
            _uiState.update { current ->
                val nextMode =
                    if (current.viewMode == FranchiseViewMode.MAP) {
                        FranchiseViewMode.LIST
                    } else {
                        FranchiseViewMode.MAP
                    }
                current.copy(viewMode = nextMode)
            }
        }

        fun onRecenterClick() {
            _uiState.update { it.copy(recenterTrigger = it.recenterTrigger + 1) }
        }

        fun onFitClick() {
            _uiState.update { it.copy(fitTrigger = it.fitTrigger + 1) }
        }

        fun onZoomInClick() {
            _uiState.update { it.copy(zoomInTrigger = it.zoomInTrigger + 1) }
        }

        fun onZoomOutClick() {
            _uiState.update { it.copy(zoomOutTrigger = it.zoomOutTrigger + 1) }
        }

        fun onOpenDetails(animeId: Long) {
            viewModelScope.launch {
                eventChannel.send(FranchiseUiEvent.NavigateToDetails(animeId))
            }
        }

        fun onBackClick() {
            viewModelScope.launch {
                eventChannel.send(FranchiseUiEvent.NavigateBack)
            }
        }

        fun onIncrementEpisode(animeId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(isUpdatingEpisode = true) }
                val result = repository.incrementWatchedEpisode(animeId)
                _uiState.update { it.copy(isUpdatingEpisode = false) }
                if (result.isFailure) {
                    eventChannel.send(
                        FranchiseUiEvent.ShowSnackbar(
                            messageRes = R.string.franchise_error_update_episode,
                            isError = true,
                        ),
                    )
                }
            }
        }

        fun onUnignoreAnime(animeId: Long) {
            viewModelScope.launch {
                repository.unmarkNotInterested(animeId)
            }
        }
    }
