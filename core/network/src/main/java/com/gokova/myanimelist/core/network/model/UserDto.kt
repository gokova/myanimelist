package com.gokova.myanimelist.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class UserDto(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String,
    @SerialName("picture")
    val picture: String? = null,
    @SerialName("gender")
    val gender: String? = null,
    @SerialName("birthday")
    val birthday: String? = null,
    @SerialName("location")
    val location: String? = null,
    @SerialName("joined_at")
    val joinedAt: String? = null,
    @SerialName("anime_statistics")
    val animeStatistics: UserAnimeStatisticsDto? = null,
    @SerialName("time_zone")
    val timeZone: String? = null,
    @SerialName("is_supporter")
    val isSupporter: Boolean? = null,
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class UserAnimeStatisticsDto(
    @SerialName("num_items_watching")
    val numItemsWatching: Int = 0,
    @SerialName("num_items_completed")
    val numItemsCompleted: Int = 0,
    @SerialName("num_items_on_hold")
    val numItemsOnHold: Int = 0,
    @SerialName("num_items_dropped")
    val numItemsDropped: Int = 0,
    @SerialName("num_items_plan_to_watch")
    val numItemsPlanToWatch: Int = 0,
    @SerialName("num_items")
    val numItems: Int = 0,
    @SerialName("num_days_watched")
    val numDaysWatched: Float = 0f,
    @SerialName("num_days_watching")
    val numDaysWatching: Float = 0f,
    @SerialName("num_days_completed")
    val numDaysCompleted: Float = 0f,
    @SerialName("num_days_on_hold")
    val numDaysOnHold: Float = 0f,
    @SerialName("num_days_dropped")
    val numDaysDropped: Float = 0f,
    @SerialName("num_days")
    val numDays: Float = 0f,
    @SerialName("num_episodes")
    val numEpisodes: Int = 0,
    @SerialName("num_times_rewatched")
    val numTimesRewatched: Int = 0,
    @SerialName("mean_score")
    val meanScore: Float = 0f,
)
