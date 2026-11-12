package com.gokova.myanimelist.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "anime_relationships",
    primaryKeys = ["source_anime_id", "target_anime_id", "relation_type"],
    indices = [
        Index(value = ["source_anime_id"]),
        Index(value = ["target_anime_id"]),
    ],
)
data class AnimeRelationshipEntity(
    @ColumnInfo(name = "source_anime_id")
    val sourceAnimeId: Long,
    @ColumnInfo(name = "target_anime_id")
    val targetAnimeId: Long,
    @ColumnInfo(name = "relation_type")
    val relationType: String,
    @ColumnInfo(name = "relation_type_formatted")
    val relationTypeFormatted: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
