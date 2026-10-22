package com.gokova.myanimelist.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gokova.myanimelist.core.database.entity.IgnoredAnimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredAnimeDao {
    @Query("SELECT anime_id FROM ignored_animes")
    fun observeIgnoredAnimeIds(): Flow<List<Long>>

    @Query("SELECT anime_id FROM ignored_animes")
    suspend fun getIgnoredAnimeIds(): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM ignored_animes WHERE anime_id = :animeId)")
    fun observeIsIgnored(animeId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM ignored_animes WHERE anime_id = :animeId)")
    suspend fun isIgnored(animeId: Long): Boolean

    @Upsert
    suspend fun upsertIgnoredAnime(entity: IgnoredAnimeEntity)

    @Query("DELETE FROM ignored_animes WHERE anime_id = :animeId")
    suspend fun deleteIgnoredAnime(animeId: Long): Int

    @Query("SELECT * FROM ignored_animes ORDER BY created_at DESC")
    fun observeAllIgnoredAnime(): Flow<List<IgnoredAnimeEntity>>

    @Query("SELECT * FROM ignored_animes ORDER BY created_at DESC")
    suspend fun getAllIgnoredAnime(): List<IgnoredAnimeEntity>
}
