package com.gokova.myanimelist.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_anime_list",
    foreignKeys = [
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["id"],
            childColumns = ["anime_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["anime_id"]),
        Index(value = ["status"]),
        Index(value = ["updated_at"]),
    ],
)
data class UserAnimeListEntity(
    @PrimaryKey
    @ColumnInfo(name = "anime_id")
    val animeId: Long,
    val status: String,
    val score: Int,
    @ColumnInfo(name = "num_episodes_watched")
    val numEpisodesWatched: Int,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_rewatching")
    val isRewatching: Boolean,
)
