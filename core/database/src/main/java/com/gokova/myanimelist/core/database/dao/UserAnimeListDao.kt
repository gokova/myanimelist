package com.gokova.myanimelist.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
interface UserAnimeListDao {
    @Transaction
    @Query("SELECT * FROM user_anime_list")
    fun observeAllUserAnime(): Flow<List<UserAnimeListItem>>

    @Transaction
    @Query("SELECT * FROM user_anime_list")
    suspend fun getAllUserAnime(): List<UserAnimeListItem>

    @Transaction
    @Query("SELECT * FROM user_anime_list WHERE status = :status")
    fun observeUserAnimeByStatus(status: String): Flow<List<UserAnimeListItem>>

    @Transaction
    @Query("SELECT * FROM user_anime_list WHERE anime_id = :animeId")
    fun observeUserAnimeById(animeId: Long): Flow<UserAnimeListItem?>

    @Transaction
    @Query("SELECT * FROM user_anime_list WHERE anime_id = :animeId")
    suspend fun getUserAnimeById(animeId: Long): UserAnimeListItem?

    @Query("SELECT * FROM animes WHERE id = :animeId")
    fun observeAnimeEntityById(animeId: Long): Flow<AnimeEntity?>

    @Query("SELECT * FROM animes WHERE id = :animeId")
    suspend fun getAnimeEntityById(animeId: Long): AnimeEntity?

    @Upsert
    suspend fun upsertAnimes(animes: List<AnimeEntity>)

    @Upsert
    suspend fun upsertUserAnimeList(userAnimeList: List<UserAnimeListEntity>)

    @Query("DELETE FROM user_anime_list")
    suspend fun clearUserAnimeList(): Int

    @Transaction
    suspend fun syncUserAnimeList(
        animes: List<AnimeEntity>,
        userAnimeList: List<UserAnimeListEntity>,
    ) {
        upsertAnimes(animes)
        clearUserAnimeList()
        upsertUserAnimeList(userAnimeList)
    }

    @Query("DELETE FROM recommendations WHERE anime_id = :animeId")
    suspend fun deleteRecommendation(animeId: Long): Int

    @Query("DELETE FROM new_season_animes WHERE anime_id = :animeId")
    suspend fun deleteNewSeasonAnime(animeId: Long): Int

    @Transaction
    suspend fun addAnimeToUserListAndClean(userAnime: UserAnimeListEntity) {
        upsertUserAnimeList(listOf(userAnime))
        deleteRecommendation(userAnime.animeId)
        deleteNewSeasonAnime(userAnime.animeId)
    }
}
