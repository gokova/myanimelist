package com.gokova.myanimelist.core.domain.model

data class UserAnimeListUpdate(
    val animeId: Long,
    val status: String,
    val score: Int,
    val numEpisodesWatched: Int,
    val isRewatching: Boolean,
    val updatedAt: String,
)
