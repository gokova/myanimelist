package com.gokova.myanimelist.core.domain.model

data class UpdateUserAnimeStatusParams(
    val status: String? = null,
    val isRewatching: Boolean? = null,
    val score: Int? = null,
    val numWatchedEpisodes: Int? = null,
)
