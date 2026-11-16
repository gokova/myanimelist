package com.gokova.myanimelist.core.domain.model

data class AnimeConflictValues(
    val episodesWatched: Int? = null,
    val status: String? = null,
    val score: Int? = null,
    val isRewatching: Boolean? = null,
)

data class AnimeConflictDetails(
    val animeId: Long,
    val baseline: AnimeConflictValues,
    val device: AnimeConflictValues,
    val remote: AnimeConflictValues,
)
