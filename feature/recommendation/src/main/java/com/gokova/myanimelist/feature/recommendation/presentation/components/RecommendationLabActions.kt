package com.gokova.myanimelist.feature.recommendation.presentation.components

import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationLabConfig

data class RecommendationLabActions(
    val onConfigChange: (RecommendationLabConfig) -> Unit = {},
    val onApply: () -> Unit = {},
    val onReset: () -> Unit = {},
    val onDismissRequest: () -> Unit = {},
)
