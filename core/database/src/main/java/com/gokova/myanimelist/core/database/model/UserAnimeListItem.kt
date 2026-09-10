package com.gokova.myanimelist.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity

data class UserAnimeListItem(
    @Embedded
    val userAnime: UserAnimeListEntity,
    @Relation(
        parentColumn = "anime_id",
        entityColumn = "id",
    )
    val anime: AnimeEntity,
)
