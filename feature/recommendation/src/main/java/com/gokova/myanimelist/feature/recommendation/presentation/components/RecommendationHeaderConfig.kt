package com.gokova.myanimelist.feature.recommendation.presentation.components

import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType

data class RecommendationHeaderConfig(
    val count: Int,
    val selectedType: RecommendationType,
    val sortConfig: HeaderSortConfig? = null,
    val compact: Boolean = false,
)
