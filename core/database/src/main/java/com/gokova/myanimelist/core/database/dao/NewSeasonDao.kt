package com.gokova.myanimelist.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.NewSeasonAnimeEntity
import com.gokova.myanimelist.core.database.model.NewSeasonAnimeItem
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
interface NewSeasonDao {
    @Transaction
    @Query("SELECT * FROM new_season_animes ORDER BY created_at DESC")
    fun observeNewSeasons(): Flow<List<NewSeasonAnimeItem>>

    @Query("SELECT COUNT(*) FROM new_season_animes")
    fun observeNewSeasonCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM new_season_animes")
    suspend fun getNewSeasonCount(): Int

    @Query(
        """
        SELECT anime_id FROM new_season_animes
        WHERE (:parentIds IS NULL OR parent_anime_id IN (:parentIds))
        """,
    )
    suspend fun getNewSeasonAnimeIds(parentIds: List<Long>? = null): List<Long>

    @Query(
        """
        SELECT anime_id FROM new_season_animes
        WHERE created_at < :timestamp
        """,
    )
    suspend fun getObsoleteNewSeasonAnimeIds(timestamp: Long): List<Long>

    @Upsert
    suspend fun upsertAnimes(animes: List<AnimeEntity>)

    @Upsert
    suspend fun upsertNewSeasons(items: List<NewSeasonAnimeEntity>)

    @Query("SELECT id FROM animes WHERE id IN (:animeIds)")
    suspend fun getExistingAnimeIds(animeIds: List<Long>): List<Long>

    @Query("DELETE FROM new_season_animes WHERE anime_id IN (:animeIds)")
    suspend fun deleteNewSeasons(animeIds: List<Long>): Int

    @Query(
        """
        DELETE FROM animes
        WHERE id IN (:animeIds)
          AND id NOT IN (SELECT anime_id FROM user_anime_list)
          AND id NOT IN (SELECT anime_id FROM recommendations)
          AND id NOT IN (SELECT anime_id FROM new_season_animes)
          AND id NOT IN (SELECT parent_anime_id FROM new_season_animes)
        """,
    )
    suspend fun deleteOrphanedAnimes(animeIds: List<Long>): Int

    @Transaction
    suspend fun saveNewSeasonsTransaction(
        animes: List<AnimeEntity> = emptyList(),
        newSeasons: List<NewSeasonAnimeEntity> = emptyList(),
        discardedAnimeIds: List<Long> = emptyList(),
    ) {
        if (animes.isNotEmpty()) {
            upsertAnimes(animes)
        }
        if (newSeasons.isNotEmpty()) {
            upsertNewSeasons(newSeasons)
        }
        if (discardedAnimeIds.isNotEmpty()) {
            deleteNewSeasons(discardedAnimeIds)
            deleteOrphanedAnimes(discardedAnimeIds)
        }
    }
}
