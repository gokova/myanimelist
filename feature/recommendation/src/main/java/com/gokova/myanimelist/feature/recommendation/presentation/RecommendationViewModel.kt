package com.gokova.myanimelist.feature.recommendation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.domain.usecase.IgnoredAnimeInteractor
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationEngineState
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationLabConfig
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime
import com.gokova.myanimelist.feature.recommendation.domain.usecase.NewSeasonsInteractor
import com.gokova.myanimelist.feature.recommendation.domain.usecase.RecommendationInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecommendationViewModel
    @Inject
    constructor(
        private val recommendations: RecommendationInteractor,
        private val newSeasons: NewSeasonsInteractor,
        private val ignoredAnimeInteractor: IgnoredAnimeInteractor,
    ) : ViewModel() {
        private val effectChannel = Channel<RecommendationSideEffect>(Channel.BUFFERED)
        val sideEffects: Flow<RecommendationSideEffect> = effectChannel.receiveAsFlow()

        private val selectedType = MutableStateFlow(RecommendationType.GENRE)
        private val newSeasonSort = MutableStateFlow(NewSeasonSortOption.RELEASE_DATE_DESC)
        private val appliedLabConfig = MutableStateFlow(RecommendationLabConfig())
        private val draftLabConfig = MutableStateFlow(RecommendationLabConfig())
        private val isLabOpen = MutableStateFlow(false)
        private val labPreviewCount = MutableStateFlow<Int?>(null)

        init {
            viewModelScope.launch {
                newSeasons.scheduleWork()
                recommendations.scheduleWork()
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
                recommendations.observeRecommendationState(),
                appliedLabConfig.flatMapLatest { config ->
                    recommendations.observeRecommendations(type, config)
                },
                draftLabConfig,
                isLabOpen,
                labPreviewCount,
            ) { engineState, recs, draftConfig, isLabOpen, previewCount ->
                mapRecommendationEngineState(
                    type = type,
                    engineState = engineState,
                    recommendations = recs,
                    labState = LabSheetState(draftConfig, isLabOpen, previewCount),
                )
            }

        private fun mapRecommendationEngineState(
            type: RecommendationType,
            engineState: RecommendationEngineState,
            recommendations: List<RecommendedAnime>,
            labState: LabSheetState,
        ): RecommendationUiState =
            when (engineState) {
                RecommendationEngineState.EmptyInsufficientData ->
                    RecommendationUiState.EmptyInsufficientData(type, labState.config)
                RecommendationEngineState.EmptyAllCaughtUp ->
                    RecommendationUiState.EmptyAllCaughtUp(type, labState.config)
                RecommendationEngineState.Calculating ->
                    RecommendationUiState.Calculating(type, labState.config)
                is RecommendationEngineState.Error ->
                    RecommendationUiState.Error(type, engineState.message, labState.config)
                is RecommendationEngineState.Ready -> {
                    if (recommendations.isEmpty()) {
                        RecommendationUiState.EmptyAllCaughtUp(type, labState.config)
                    } else {
                        RecommendationUiState.Success(
                            selectedType = type,
                            recommendations = recommendations,
                            isRefreshing = engineState.isRefreshing,
                            isQueued = engineState.isQueued,
                            labConfig = labState.config,
                            isLabOpen = labState.isOpen,
                            labPreviewCount = labState.previewCount,
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
                            isRefreshing = state.isRefreshing,
                            isQueued = state.isQueued,
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
                is RecommendationUiEvent.MarkNotInterested -> handleMarkNotInterested(event)
                is RecommendationUiEvent.UndoNotInterested -> handleUndoNotInterested(event)
                RecommendationUiEvent.OpenLab -> {
                    draftLabConfig.value = appliedLabConfig.value
                    isLabOpen.value = true
                    updateLabPreviewCount(selectedType.value, draftLabConfig.value)
                }
                RecommendationUiEvent.CloseLab -> {
                    draftLabConfig.value = appliedLabConfig.value
                    isLabOpen.value = false
                    labPreviewCount.value = null
                }
                is RecommendationUiEvent.UpdateLabDraft -> {
                    draftLabConfig.value = event.config
                    updateLabPreviewCount(selectedType.value, event.config)
                }
                is RecommendationUiEvent.ApplyLabConfig -> {
                    appliedLabConfig.value = event.config
                    draftLabConfig.value = event.config
                    isLabOpen.value = false
                    labPreviewCount.value = null
                }
                RecommendationUiEvent.ResetLabConfig -> {
                    appliedLabConfig.value = RecommendationLabConfig()
                    draftLabConfig.value = RecommendationLabConfig()
                    isLabOpen.value = false
                    labPreviewCount.value = null
                }
            }
        }

        private fun updateLabPreviewCount(
            type: RecommendationType,
            config: RecommendationLabConfig,
        ) {
            viewModelScope.launch {
                try {
                    val count = recommendations.calculateLabMatchCount(type, config)
                    labPreviewCount.value = count
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.ui.e(e) { "Failed to calculate lab match count" }
                }
            }
        }

        private fun handleMarkNotInterested(event: RecommendationUiEvent.MarkNotInterested) {
            AppLog.ui.i { "User marked anime ${event.animeId} as not interested" }
            viewModelScope.launch {
                try {
                    ignoredAnimeInteractor.setNotInterested(
                        animeId = event.animeId,
                        title = event.title,
                        titleEnglish = event.titleEnglish,
                        pictureUrl = event.pictureUrl,
                    )
                    effectChannel.send(
                        RecommendationSideEffect.ShowUndoNotInterestedSnackbar(
                            animeId = event.animeId,
                            animeTitle = event.titleEnglish ?: event.title,
                        ),
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.ui.e(e) { "Failed to mark anime as not interested" }
                }
            }
        }

        private fun handleUndoNotInterested(event: RecommendationUiEvent.UndoNotInterested) {
            AppLog.ui.i { "User undo not interested for anime ${event.animeId}" }
            viewModelScope.launch {
                try {
                    ignoredAnimeInteractor.removeNotInterested(event.animeId)
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.ui.e(e) { "Failed to undo not interested" }
                }
            }
        }

        private fun handleRefresh() {
            val isBusy =
                when (val currentState = uiState.value) {
                    is RecommendationUiState.Calculating -> true
                    is RecommendationUiState.Success ->
                        currentState.isRefreshing || currentState.isQueued
                    else -> false
                }
            if (isBusy) {
                AppLog.ui.i { "Suppressing duplicate recalculation for ${selectedType.value}" }
                return
            }
            AppLog.ui.i { "User triggered recalculation for type: ${selectedType.value}" }
            viewModelScope.launch {
                try {
                    if (selectedType.value == RecommendationType.NEW_SEASONS) {
                        newSeasons.triggerCalculation()
                    } else {
                        recommendations.triggerCalculation()
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

        private data class LabSheetState(
            val config: RecommendationLabConfig,
            val isOpen: Boolean,
            val previewCount: Int?,
        )
    }
