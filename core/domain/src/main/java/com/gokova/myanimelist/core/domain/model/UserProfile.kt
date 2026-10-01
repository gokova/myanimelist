package com.gokova.myanimelist.core.domain.model

data class UserProfile(
    val id: Long,
    val name: String,
    val pictureUrl: String? = null,
    val gender: String? = null,
    val birthday: String? = null,
    val location: String? = null,
    val joinedAt: String? = null,
    val statistics: UserAnimeStatistics? = null,
)

data class UserAnimeStatistics(
    val numItemsWatching: Int = 0,
    val numItemsCompleted: Int = 0,
    val numItemsOnHold: Int = 0,
    val numItemsDropped: Int = 0,
    val numItemsPlanToWatch: Int = 0,
    val numItems: Int = 0,
    val numDaysWatched: Float = 0f,
    val numDaysWatching: Float = 0f,
    val numDaysCompleted: Float = 0f,
    val numDaysOnHold: Float = 0f,
    val numDaysDropped: Float = 0f,
    val numDays: Float = 0f,
    val numEpisodes: Int = 0,
    val numTimesRewatched: Int = 0,
    val meanScore: Float = 0f,
)
