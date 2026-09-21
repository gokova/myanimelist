package com.gokova.myanimelist.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.RecommendationCandidateEntity
import com.gokova.myanimelist.core.database.entity.RecommendationEntity
import com.gokova.myanimelist.core.database.model.RecommendationItem
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
interface RecommendationDao {
    @Transaction
    @Query("SELECT * FROM recommendations ORDER BY genre_rank ASC LIMIT :limit")
    fun observeRecommendationsByGenre(limit: Int = 500): Flow<List<RecommendationItem>>

    @Transaction
    @Query("SELECT * FROM recommendations ORDER BY theme_rank ASC LIMIT :limit")
    fun observeRecommendationsByTheme(limit: Int = 500): Flow<List<RecommendationItem>>

    @Query("SELECT COUNT(*) FROM recommendations")
    fun observeRecommendationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recommendations")
    suspend fun getRecommendationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnimes(animes: List<AnimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCandidates(candidates: List<RecommendationCandidateEntity>)

    @Query("SELECT anime_id FROM user_anime_list")
    suspend fun getUserAnimeIds(): List<Long>

    @Query(
        """
        SELECT anime_id FROM user_anime_list
        UNION
        SELECT anime_id FROM new_season_animes
        """,
    )
    suspend fun getExcludedCandidateAnimeIds(): List<Long>

    @Transaction
    @Query(
        """
        SELECT a.* FROM animes a
        INNER JOIN recommendation_candidates rc ON a.id = rc.anime_id
        """,
    )
    suspend fun getCandidateAnimes(): List<AnimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecommendations(recommendations: List<RecommendationEntity>)

    @Query("DELETE FROM recommendations")
    suspend fun clearRecommendations(): Int

    @Query("DELETE FROM recommendation_candidates")
    suspend fun clearCandidates(): Int

    @Query(
        """
        DELETE FROM animes
        WHERE id IN (:animeIds)
          AND id NOT IN (SELECT anime_id FROM user_anime_list)
          AND id NOT IN (SELECT anime_id FROM new_season_animes)
        """,
    )
    suspend fun deleteNonUserData(animeIds: List<Long>): Int

    @Transaction
    suspend fun saveEvaluatedRecommendations(
        recommendations: List<RecommendationEntity>,
        discardedAnimeIds: List<Long>,
    ) {
        clearRecommendations()
        upsertRecommendations(recommendations)
        if (discardedAnimeIds.isNotEmpty()) {
            deleteNonUserData(discardedAnimeIds)
        }
        clearCandidates()
    }
}
