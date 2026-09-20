package com.gokova.myanimelist.feature.recommendation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationEngineState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ObserveRecommendationStateUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ObserveRecommendationsUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.ScheduleRecommendationWorkUseCase
import com.gokova.myanimelist.feature.recommendation.domain.usecase.TriggerRecommendationCalculationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    ) : ViewModel() {
        private val selectedType = MutableStateFlow(RecommendationType.GENRE)

        init {
            viewModelScope.launch {
                scheduleRecommendationWorkUseCase()
            }
            viewModelScope.launch {
                observeRecommendationStateUseCase().collect { state ->
                    if (state is RecommendationEngineState.Calculating) {
                        scheduleRecommendationWorkUseCase()
                    }
                }
            }
        }

        val uiState: StateFlow<RecommendationUiState> =
            combine(
                observeRecommendationStateUseCase(),
                selectedType.flatMapLatest { type ->
                    observeRecommendationsUseCase(type)
                },
                selectedType,
            ) { engineState, recommendations, type ->
                when (engineState) {
                    RecommendationEngineState.EmptyInsufficientData ->
                        RecommendationUiState.EmptyInsufficientData
                    RecommendationEngineState.Calculating ->
                        RecommendationUiState.Calculating
                    is RecommendationEngineState.Error ->
                        RecommendationUiState.Error(engineState.message)
                    is RecommendationEngineState.Ready -> {
                        if (recommendations.isEmpty()) {
                            RecommendationUiState.Calculating
                        } else {
                            RecommendationUiState.Success(
                                selectedType = type,
                                recommendations = recommendations,
                            )
                        }
                    }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = RecommendationUiState.Loading,
            )

        fun onEvent(event: RecommendationUiEvent) {
            when (event) {
                is RecommendationUiEvent.SelectType -> {
                    AppLog.ui.i { "User selected recommendation type: ${event.type}" }
                    selectedType.value = event.type
                }
                RecommendationUiEvent.CalculateNow,
                RecommendationUiEvent.Refresh,
                -> {
                    AppLog.ui.i { "User triggered recommendation recalculation" }
                    viewModelScope.launch {
                        try {
                            triggerCalculationUseCase()
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
        }
    }
