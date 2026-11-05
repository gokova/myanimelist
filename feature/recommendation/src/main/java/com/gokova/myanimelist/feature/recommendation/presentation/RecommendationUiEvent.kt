package com.gokova.myanimelist.feature.recommendation.presentation

import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationLabConfig
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType

sealed interface RecommendationUiEvent {
    data class SelectType(
        val type: RecommendationType,
    ) : RecommendationUiEvent

    data class SelectNewSeasonSort(
        val sort: NewSeasonSortOption,
    ) : RecommendationUiEvent

    data object CalculateNow : RecommendationUiEvent

    data object Refresh : RecommendationUiEvent

    data class MarkNotInterested(
        val animeId: Long,
        val title: String,
        val titleEnglish: String? = null,
        val pictureUrl: String? = null,
    ) : RecommendationUiEvent

    data class UndoNotInterested(
        val animeId: Long,
    ) : RecommendationUiEvent

    data object OpenLab : RecommendationUiEvent

    data object CloseLab : RecommendationUiEvent

    data class UpdateLabDraft(
        val config: RecommendationLabConfig,
    ) : RecommendationUiEvent

    data class ApplyLabConfig(
        val config: RecommendationLabConfig,
    ) : RecommendationUiEvent

    data object ResetLabConfig : RecommendationUiEvent
}
