package com.gokova.myanimelist.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gokova.myanimelist.core.database.converter.AnimeTypeConverters
import com.gokova.myanimelist.core.database.dao.RecommendationDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.RecommendationCandidateEntity
import com.gokova.myanimelist.core.database.entity.RecommendationEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity

@Database(
    entities = [
        AnimeEntity::class,
        UserAnimeListEntity::class,
        RecommendationEntity::class,
        RecommendationCandidateEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(AnimeTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAnimeListDao(): UserAnimeListDao

    abstract fun recommendationDao(): RecommendationDao

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

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `recommendations` (
                            `anime_id` INTEGER NOT NULL,
                            `genre_score` REAL NOT NULL,
                            `genre_rank` INTEGER NOT NULL,
                            `theme_score` REAL NOT NULL,
                            `theme_rank` INTEGER NOT NULL,
                            `genre_match_percent` INTEGER NOT NULL,
                            `theme_match_percent` INTEGER NOT NULL,
                            `calculated_at` INTEGER NOT NULL,
                            PRIMARY KEY(`anime_id`),
                            FOREIGN KEY(`anime_id`) REFERENCES `animes`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_recommendations_anime_id` ON `recommendations` (`anime_id`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS " +
                            "`index_recommendations_genre_rank` ON `recommendations` (`genre_rank`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS " +
                            "`index_recommendations_theme_rank` ON `recommendations` (`theme_rank`)",
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `recommendation_candidates` (
                            `anime_id` INTEGER NOT NULL,
                            `source` TEXT NOT NULL,
                            PRIMARY KEY(`anime_id`),
                            FOREIGN KEY(`anime_id`) REFERENCES `animes`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_recommendation_candidates_anime_id` ON " +
                            "`recommendation_candidates` (`anime_id`)",
                    )
                }
            }
    }
}
