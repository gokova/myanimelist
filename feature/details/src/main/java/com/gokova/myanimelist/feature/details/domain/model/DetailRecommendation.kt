package com.gokova.myanimelist.feature.details.domain.model

data class DetailRecommendation(
    val id: Long,
    val title: String,
    val thumbnailUrl: String?,
    val meanScore: Double?,
    val numRecommendations: Int,
)
