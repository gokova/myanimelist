package com.gokova.myanimelist.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ignored_animes")
data class IgnoredAnimeEntity(
    @PrimaryKey
    @ColumnInfo(name = "anime_id")
    val animeId: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "title_english")
    val titleEnglish: String? = null,
    @ColumnInfo(name = "picture_url")
    val pictureUrl: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
