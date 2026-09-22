package com.gokova.myanimelist.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "new_season_animes",
    foreignKeys = [
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["id"],
            childColumns = ["anime_id"],
        ),
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_anime_id"],
        ),
    ],
    indices = [
        Index(value = ["anime_id"], unique = true),
        Index(value = ["parent_anime_id"]),
    ],
)
data class NewSeasonAnimeEntity(
    @PrimaryKey
    @ColumnInfo(name = "anime_id")
    val animeId: Long,
    @ColumnInfo(name = "parent_anime_id")
    val parentAnimeId: Long,
    @ColumnInfo(name = "relation_type")
    val relationType: String,
    @ColumnInfo(name = "relation_type_formatted")
    val relationTypeFormatted: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
