package com.gokova.myanimelist.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gokova.myanimelist.core.database.converter.AnimeTypeConverters
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity

@Database(
    entities = [
        AnimeEntity::class,
        UserAnimeListEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(AnimeTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAnimeListDao(): UserAnimeListDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE animes ADD COLUMN genres TEXT")
                    db.execSQL("ALTER TABLE animes ADD COLUMN studios TEXT")
                    db.execSQL("ALTER TABLE animes ADD COLUMN source TEXT")
                    db.execSQL("ALTER TABLE animes ADD COLUMN synopsis TEXT")
                    db.execSQL("ALTER TABLE animes ADD COLUMN rating TEXT")
                    db.execSQL("ALTER TABLE animes ADD COLUMN rank INTEGER")
                    db.execSQL("ALTER TABLE animes ADD COLUMN popularity INTEGER")
                    db.execSQL("ALTER TABLE animes ADD COLUMN num_list_users INTEGER")
                    db.execSQL(
                        "ALTER TABLE animes ADD COLUMN average_episode_duration INTEGER",
                    )
                    db.execSQL("ALTER TABLE animes ADD COLUMN nsfw TEXT")
                }
            }
    }
}
