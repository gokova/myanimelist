package com.gokova.myanimelist.feature.taste.domain.model

data class UserAnimeRecord(
    val animeId: Long,
    val title: String,
    val imageUrl: String?,
    val status: String,
    val userScore: Int,
    val numEpisodesWatched: Int,
    val totalEpisodes: Int?,
    val genres: List<String>,
)
