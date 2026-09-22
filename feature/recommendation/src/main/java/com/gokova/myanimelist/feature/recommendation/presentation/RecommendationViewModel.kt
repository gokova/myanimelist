package com.gokova.myanimelist.feature.recommendation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationEngineState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime
import com.gokova.myanimelist.feature.recommendation.domain.usecase.NewSeasonsInteractor
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ObserveRecommendationStateUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ObserveRecommendationsUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ScheduleRecommendationWorkUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.TriggerRecommendationCalculationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecommendationViewModel
    @Inject
    constructor(
        private val observeRecommendationsUseCase: ObserveRecommendationsUseCase,
        private val observeRecommendationStateUseCase: ObserveRecommendationStateUseCase,
        private val triggerCalculationUseCase: TriggerRecommendationCalculationUseCase,
        private val scheduleRecommendationWorkUseCase: ScheduleRecommendationWorkUseCase,
        private val newSeasons: NewSeasonsInteractor,
        private val permissionManager: BackgroundSyncPermissionManager,
    ) : ViewModel() {
        private val selectedType = MutableStateFlow(RecommendationType.GENRE)
        private val newSeasonSort = MutableStateFlow(NewSeasonSortOption.RELEASE_DATE_DESC)
        private val _showBackgroundSyncPrompt = MutableStateFlow(false)
        val showBackgroundSyncPrompt: StateFlow<Boolean> = _showBackgroundSyncPrompt.asStateFlow()

        init {
            if (permissionManager.shouldPrompt()) {
                _showBackgroundSyncPrompt.value = true
            }
            viewModelScope.launch {
                scheduleRecommendationWorkUseCase()
                newSeasons.scheduleWork()
            }
        }

        val uiState: StateFlow<RecommendationUiState> =
            selectedType
                .flatMapLatest { type ->
                    if (type == RecommendationType.NEW_SEASONS) {
                        buildNewSeasonFlow()
                    } else {
                        buildRecommendationFlow(type)
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = RecommendationUiState.Loading(selectedType.value),
                )

        private fun buildRecommendationFlow(type: RecommendationType): Flow<RecommendationUiState> =
            combine(
                observeRecommendationStateUseCase(),
                observeRecommendationsUseCase(type),
            ) { engineState, recommendations ->
                mapRecommendationEngineState(type, engineState, recommendations)
            }

        private fun mapRecommendationEngineState(
            type: RecommendationType,
            engineState: RecommendationEngineState,
            recommendations: List<RecommendedAnime>,
        ): RecommendationUiState =
            when (engineState) {
                RecommendationEngineState.EmptyInsufficientData ->
                    RecommendationUiState.EmptyInsufficientData(type)
                RecommendationEngineState.Calculating ->
                    RecommendationUiState.Calculating(type)
                is RecommendationEngineState.Error ->
                    RecommendationUiState.Error(type, engineState.message)
                is RecommendationEngineState.Ready -> {
                    if (recommendations.isEmpty()) {
                        RecommendationUiState.Calculating(type)
                    } else {
                        RecommendationUiState.Success(
                            selectedType = type,
                            recommendations = recommendations,
                        )
                    }
                }
            }

        private fun buildNewSeasonFlow(): Flow<RecommendationUiState> =
            combine(
                newSeasons.observeNewSeasonState(),
                newSeasonSort.flatMapLatest { sort ->
                    newSeasons.observeNewSeasons(sort)
                },
                newSeasonSort,
            ) { state, seasons, sort ->
                mapNewSeasonState(state, seasons, sort)
            }

        private fun mapNewSeasonState(
            state: NewSeasonState,
            seasons: List<NewSeasonAnime>,
            sort: NewSeasonSortOption,
        ): RecommendationUiState =
            when (state) {
                NewSeasonState.EmptyInsufficientData ->
                    RecommendationUiState.EmptyInsufficientData(RecommendationType.NEW_SEASONS)
                NewSeasonState.EmptyAllCaughtUp ->
                    RecommendationUiState.EmptyAllCaughtUp()
                NewSeasonState.Calculating ->
                    RecommendationUiState.Calculating(RecommendationType.NEW_SEASONS)
                is NewSeasonState.Error ->
                    RecommendationUiState.Error(RecommendationType.NEW_SEASONS, state.message)
                is NewSeasonState.Ready -> {
                    if (seasons.isEmpty()) {
                        RecommendationUiState.EmptyAllCaughtUp()
                    } else {
                        RecommendationUiState.Success(
                            selectedType = RecommendationType.NEW_SEASONS,
                            newSeasons = seasons,
                            newSeasonSort = sort,
                        )
                    }
                }
            }

        fun onEvent(event: RecommendationUiEvent) {
            when (event) {
                is RecommendationUiEvent.SelectType -> {
                    AppLog.ui.i { "User selected recommendation type: ${event.type}" }
                    selectedType.value = event.type
                }
                is RecommendationUiEvent.SelectNewSeasonSort -> {
                    AppLog.ui.i { "User selected new season sort: ${event.sort}" }
                    newSeasonSort.value = event.sort
                }
                RecommendationUiEvent.CalculateNow,
                RecommendationUiEvent.Refresh,
                -> handleRefresh()
                RecommendationUiEvent.ConfirmBackgroundSyncPrompt -> {
                    AppLog.ui.i { "User confirmed background sync prompt" }
                    permissionManager.markPrompted()
                    _showBackgroundSyncPrompt.value = false
                }
                RecommendationUiEvent.DismissBackgroundSyncPrompt -> {
                    AppLog.ui.i { "User dismissed background sync prompt" }
                    permissionManager.markPrompted()
                    _showBackgroundSyncPrompt.value = false
                }
            }
        }

        fun getPermissionIntent() = permissionManager.createPermissionIntent()

        private fun handleRefresh() {
            AppLog.ui.i { "User triggered recalculation for type: ${selectedType.value}" }
            viewModelScope.launch {
                try {
                    if (selectedType.value == RecommendationType.NEW_SEASONS) {
                        newSeasons.triggerCalculation()
                    } else {
                        triggerCalculationUseCase()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.ui.e(e) { "Failed to trigger recalculation" }
                }
            }
        }
    }
