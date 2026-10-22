package com.gokova.myanimelist.core.domain.model

data class IgnoredAnime(
    val animeId: Long,
    val title: String,
    val titleEnglish: String? = null,
    val pictureUrl: String? = null,
    val createdAt: Long,
)
