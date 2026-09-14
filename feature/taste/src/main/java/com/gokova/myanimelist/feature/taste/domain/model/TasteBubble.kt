package com.gokova.myanimelist.feature.taste.domain.model

data class TasteBubble(
    val name: String,
    val count: Int,
    val radius: Float,
    val x: Float,
    val y: Float,
    val colorIndex: Int,
    val averageScore: Double?,
    val matchingAnime: List<TasteAnimeItem>,
)
