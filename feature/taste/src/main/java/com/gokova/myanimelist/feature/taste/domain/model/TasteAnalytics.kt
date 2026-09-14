package com.gokova.myanimelist.feature.taste.domain.model

data class TasteAnalytics(
    val genres: List<TasteBubble>,
    val themes: List<TasteBubble>,
    val totalAnimeCount: Int,
)
