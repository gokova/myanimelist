package com.gokova.myanimelist.feature.recommendation.presentation.components

data class NotInterestedSheetData(
    val animeId: Long,
    val title: String,
    val titleEnglish: String? = null,
    val pictureUrl: String? = null,
)
