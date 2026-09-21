package com.gokova.myanimelist.feature.recommendation.presentation

import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime

sealed interface RecommendationUiState {
    val selectedType: RecommendationType

    data class Loading(
        override val selectedType: RecommendationType = RecommendationType.GENRE,
    ) : RecommendationUiState

    data class EmptyInsufficientData(
        override val selectedType: RecommendationType = RecommendationType.GENRE,
    ) : RecommendationUiState

    data class EmptyAllCaughtUp(
        override val selectedType: RecommendationType = RecommendationType.NEW_SEASONS,
    ) : RecommendationUiState

    data class Calculating(
        override val selectedType: RecommendationType = RecommendationType.GENRE,
    ) : RecommendationUiState

    data class Success(
        override val selectedType: RecommendationType,
        val recommendations: List<RecommendedAnime> = emptyList(),
        val newSeasons: List<NewSeasonAnime> = emptyList(),
        val newSeasonSort: NewSeasonSortOption = NewSeasonSortOption.RELEASE_DATE_DESC,
    ) : RecommendationUiState

    data class Error(
        override val selectedType: RecommendationType = RecommendationType.GENRE,
        val message: String,
    ) : RecommendationUiState
}
