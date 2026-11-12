package com.gokova.myanimelist.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.AnimeRelationshipEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeRelationshipDao {
    @Upsert
    suspend fun upsertRelationships(relationships: List<AnimeRelationshipEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnimeStubs(animes: List<AnimeEntity>)

    @Transaction
    suspend fun saveRelationships(
        relationships: List<AnimeRelationshipEntity>,
        animeStubs: List<AnimeEntity> = emptyList(),
    ) {
        if (animeStubs.isNotEmpty()) {
            insertAnimeStubs(animeStubs)
        }
        if (relationships.isNotEmpty()) {
            upsertRelationships(relationships)
        }
    }

    @Query(
        "SELECT EXISTS(" +
            "SELECT 1 FROM anime_relationships " +
            "WHERE source_anime_id = :animeId OR target_anime_id = :animeId" +
            ")",
    )
    fun observeHasRelationships(animeId: Long): Flow<Boolean>

    @Query(
        "SELECT * FROM anime_relationships " +
            "WHERE source_anime_id IN (:animeIds) OR target_anime_id IN (:animeIds)",
    )
    suspend fun getRelationshipsForAnimeIds(animeIds: Set<Long>): List<AnimeRelationshipEntity>

    @Query("SELECT * FROM animes WHERE id IN (:animeIds)")
    fun observeAnimeEntitiesByIds(animeIds: Set<Long>): Flow<List<AnimeEntity>>

    @Query("SELECT id FROM animes WHERE id IN (:animeIds)")
    suspend fun getExistingAnimeIds(animeIds: Set<Long>): List<Long>

    @Transaction
    @Query("SELECT * FROM user_anime_list WHERE anime_id IN (:animeIds)")
    fun observeUserAnimeByIds(animeIds: Set<Long>): Flow<List<UserAnimeListItem>>
}
