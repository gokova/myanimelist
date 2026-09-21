package com.gokova.myanimelist.feature.taste.domain.model

data class TasteAnimeItem(
    val id: Long,
    val title: String,
    val thumbnailUrl: String?,
    val largeImageUrl: String?,
    val userScore: Int,
    val userStatus: String,
    val totalEpisodes: Int?,
    val watchedEpisodes: Int,
)
