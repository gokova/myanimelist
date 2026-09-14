package com.gokova.myanimelist.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animes")
data class AnimeEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    @ColumnInfo(name = "title_english")
    val titleEnglish: String?,
    @ColumnInfo(name = "main_picture_medium")
    val mainPictureMedium: String?,
    @ColumnInfo(name = "main_picture_large")
    val mainPictureLarge: String?,
    @ColumnInfo(name = "media_type")
    val mediaType: String?,
    @ColumnInfo(name = "airing_status")
    val airingStatus: String?,
    @ColumnInfo(name = "num_episodes")
    val numEpisodes: Int?,
    @ColumnInfo(name = "start_season_year")
    val startSeasonYear: Int?,
    @ColumnInfo(name = "start_season_season")
    val startSeasonSeason: String?,
    @ColumnInfo(name = "mean_score")
    val meanScore: Double?,
    @ColumnInfo(name = "genres")
    val genres: List<GenreEntity>? = null,
    @ColumnInfo(name = "studios")
    val studios: List<StudioEntity>? = null,
    @ColumnInfo(name = "source")
    val source: String? = null,
    @ColumnInfo(name = "synopsis")
    val synopsis: String? = null,
    @ColumnInfo(name = "rating")
    val rating: String? = null,
    @ColumnInfo(name = "rank")
    val rank: Int? = null,
    @ColumnInfo(name = "popularity")
    val popularity: Int? = null,
    @ColumnInfo(name = "num_list_users")
    val numListUsers: Int? = null,
    @ColumnInfo(name = "average_episode_duration")
    val averageEpisodeDuration: Int? = null,
    @ColumnInfo(name = "nsfw")
    val nsfw: String? = null,
)
