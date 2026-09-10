package com.gokova.myanimelist.feature.mylist.domain.model

data class UserAnime(
    val id: Long,
    val originalTitle: String,
    val englishTitle: String?,
    val displayTitle: String,
    val subtitleTitle: String?,
    val imageUrl: String?,
    val mediaType: String?,
    val airingStatus: AiringStatus,
    val releaseSeason: String?,
    val totalEpisodes: Int?,
    val userStatus: UserAnimeStatus,
    val userScore: Int,
    val watchedEpisodes: Int,
    val isRewatching: Boolean,
    val updatedAt: String,
)
