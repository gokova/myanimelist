package com.gokova.myanimelist.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAnimeListDao {
    @Transaction
    @Query("SELECT * FROM user_anime_list")
    fun observeAllUserAnime(): Flow<List<UserAnimeListItem>>

    @Transaction
    @Query("SELECT * FROM user_anime_list WHERE status = :status")
    fun observeUserAnimeByStatus(status: String): Flow<List<UserAnimeListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnimes(animes: List<AnimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
}
