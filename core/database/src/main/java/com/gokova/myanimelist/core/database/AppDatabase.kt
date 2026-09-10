package com.gokova.myanimelist.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity

@Database(
    entities = [
        AnimeEntity::class,
        UserAnimeListEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAnimeListDao(): UserAnimeListDao
}
