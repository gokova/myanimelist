package com.gokova.myanimelist.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gokova.myanimelist.core.database.converter.AnimeTypeConverters
import com.gokova.myanimelist.core.database.dao.NewSeasonDao
import com.gokova.myanimelist.core.database.dao.RecommendationDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.NewSeasonAnimeEntity
import com.gokova.myanimelist.core.database.entity.RecommendationCandidateEntity
import com.gokova.myanimelist.core.database.entity.RecommendationEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity

@Database(
    entities = [
        AnimeEntity::class,
        UserAnimeListEntity::class,
        RecommendationEntity::class,
        RecommendationCandidateEntity::class,
        NewSeasonAnimeEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(AnimeTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAnimeListDao(): UserAnimeListDao

    abstract fun recommendationDao(): RecommendationDao

    abstract fun newSeasonDao(): NewSeasonDao

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

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `new_season_animes` (
                            `anime_id` INTEGER NOT NULL,
                            `parent_anime_id` INTEGER NOT NULL,
                            `relation_type` TEXT NOT NULL,
                            `relation_type_formatted` TEXT NOT NULL,
                            `created_at` INTEGER NOT NULL,
                            PRIMARY KEY(`anime_id`),
                            FOREIGN KEY(`anime_id`) REFERENCES `animes`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(`parent_anime_id`) REFERENCES `animes`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_new_season_animes_anime_id` ON " +
                            "`new_season_animes` (`anime_id`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS " +
                            "`index_new_season_animes_parent_anime_id` ON " +
                            "`new_season_animes` (`parent_anime_id`)",
                    )
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    recreateRecommendationsTable(db)
                    recreateRecommendationCandidatesTable(db)
                    recreateNewSeasonAnimesTable(db)
                }

                private fun recreateRecommendationsTable(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `recommendations_new` (
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
                                ON UPDATE NO ACTION ON DELETE NO ACTION
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("INSERT INTO `recommendations_new` SELECT * FROM `recommendations`")
                    db.execSQL("DROP TABLE `recommendations`")
                    db.execSQL("ALTER TABLE `recommendations_new` RENAME TO `recommendations`")
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
                }

                private fun recreateRecommendationCandidatesTable(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `recommendation_candidates_new` (
                            `anime_id` INTEGER NOT NULL,
                            `source` TEXT NOT NULL,
                            PRIMARY KEY(`anime_id`),
                            FOREIGN KEY(`anime_id`) REFERENCES `animes`(`id`)
                                ON UPDATE NO ACTION ON DELETE NO ACTION
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "INSERT INTO `recommendation_candidates_new` SELECT * FROM `recommendation_candidates`",
                    )
                    db.execSQL("DROP TABLE `recommendation_candidates`")
                    db.execSQL(
                        "ALTER TABLE `recommendation_candidates_new` RENAME TO `recommendation_candidates`",
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_recommendation_candidates_anime_id` ON " +
                            "`recommendation_candidates` (`anime_id`)",
                    )
                }

                private fun recreateNewSeasonAnimesTable(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `new_season_animes_new` (
                            `anime_id` INTEGER NOT NULL,
                            `parent_anime_id` INTEGER NOT NULL,
                            `relation_type` TEXT NOT NULL,
                            `relation_type_formatted` TEXT NOT NULL,
                            `created_at` INTEGER NOT NULL,
                            PRIMARY KEY(`anime_id`),
                            FOREIGN KEY(`anime_id`) REFERENCES `animes`(`id`)
                                ON UPDATE NO ACTION ON DELETE NO ACTION,
                            FOREIGN KEY(`parent_anime_id`) REFERENCES `animes`(`id`)
                                ON UPDATE NO ACTION ON DELETE NO ACTION
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("INSERT INTO `new_season_animes_new` SELECT * FROM `new_season_animes`")
                    db.execSQL("DROP TABLE `new_season_animes`")
                    db.execSQL("ALTER TABLE `new_season_animes_new` RENAME TO `new_season_animes`")
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_new_season_animes_anime_id` ON " +
                            "`new_season_animes` (`anime_id`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS " +
                            "`index_new_season_animes_parent_anime_id` ON " +
                            "`new_season_animes` (`parent_anime_id`)",
                    )
                }
            }
    }
}
