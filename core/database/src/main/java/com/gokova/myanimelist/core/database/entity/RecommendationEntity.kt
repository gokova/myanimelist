package com.gokova.myanimelist.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recommendations",
    foreignKeys = [
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["id"],
            childColumns = ["anime_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["anime_id"], unique = true),
        Index(value = ["genre_rank"]),
        Index(value = ["theme_rank"]),
    ],
)
data class RecommendationEntity(
    @PrimaryKey
    @ColumnInfo(name = "anime_id")
    val animeId: Long,
    @ColumnInfo(name = "genre_score")
    val genreScore: Double,
    @ColumnInfo(name = "genre_rank")
    val genreRank: Int,
    @ColumnInfo(name = "theme_score")
    val themeScore: Double,
    @ColumnInfo(name = "theme_rank")
    val themeRank: Int,
    @ColumnInfo(name = "genre_match_percent")
    val genreMatchPercent: Int,
    @ColumnInfo(name = "theme_match_percent")
    val themeMatchPercent: Int,
    @ColumnInfo(name = "calculated_at")
    val calculatedAt: Long = System.currentTimeMillis(),
)
