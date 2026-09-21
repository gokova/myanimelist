package com.gokova.myanimelist.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.NewSeasonAnimeEntity

data class NewSeasonAnimeItem(
    @Embedded
    val newSeason: NewSeasonAnimeEntity,
    @Relation(
        parentColumn = "anime_id",
        entityColumn = "id",
    )
    val anime: AnimeEntity,
    @Relation(
        parentColumn = "parent_anime_id",
        entityColumn = "id",
    )
    val parentAnime: AnimeEntity,
)
