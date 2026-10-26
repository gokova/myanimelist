package com.gokova.myanimelist.feature.recommendation.presentation.components

data class RecommendationCardActions(
    val onCardClick: (() -> Unit)? = null,
    val onCardLongClick: (() -> Unit)? = null,
    val onPosterClick: (() -> Unit)? = null,
    val onMoreClick: (() -> Unit)? = null,
)
