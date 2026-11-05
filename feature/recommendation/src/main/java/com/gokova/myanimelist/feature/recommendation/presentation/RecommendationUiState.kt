package com.gokova.myanimelist.feature.recommendation.presentation

import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationLabConfig
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime

sealed interface RecommendationUiState {
    val selectedType: RecommendationType
    val labConfig: RecommendationLabConfig
        get() = RecommendationLabConfig()
    val isLabOpen: Boolean
        get() = false
    val labPreviewCount: Int?
        get() = null

    data class Loading(
        override val selectedType: RecommendationType = RecommendationType.GENRE,
        override val labConfig: RecommendationLabConfig = RecommendationLabConfig(),
    ) : RecommendationUiState

    data class EmptyInsufficientData(
        override val selectedType: RecommendationType = RecommendationType.GENRE,
        override val labConfig: RecommendationLabConfig = RecommendationLabConfig(),
    ) : RecommendationUiState

    data class EmptyAllCaughtUp(
        override val selectedType: RecommendationType = RecommendationType.NEW_SEASONS,
        override val labConfig: RecommendationLabConfig = RecommendationLabConfig(),
    ) : RecommendationUiState

    data class Calculating(
        override val selectedType: RecommendationType = RecommendationType.GENRE,
        override val labConfig: RecommendationLabConfig = RecommendationLabConfig(),
    ) : RecommendationUiState

    data class Success(
        override val selectedType: RecommendationType,
        val recommendations: List<RecommendedAnime> = emptyList(),
        val newSeasons: List<NewSeasonAnime> = emptyList(),
        val newSeasonSort: NewSeasonSortOption = NewSeasonSortOption.RELEASE_DATE_DESC,
        val isRefreshing: Boolean = false,
        val isQueued: Boolean = false,
        override val labConfig: RecommendationLabConfig = RecommendationLabConfig(),
        override val isLabOpen: Boolean = false,
        override val labPreviewCount: Int? = null,
    ) : RecommendationUiState

    data class Error(
        override val selectedType: RecommendationType = RecommendationType.GENRE,
        val message: String,
        override val labConfig: RecommendationLabConfig = RecommendationLabConfig(),
    ) : RecommendationUiState
}
