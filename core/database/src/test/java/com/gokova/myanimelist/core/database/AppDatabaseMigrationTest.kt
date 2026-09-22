package com.gokova.myanimelist.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class AppDatabaseMigrationTest {
    @Test
    fun `migration 1 to 2 has correct version boundaries`() {
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)
    }

    @Test
    fun `migration 1 to 2 executes all 10 alter table statements in order`() {
        val executedStatements = mutableListOf<String>()

        val fakeDatabase =
            Proxy.newProxyInstance(
                SupportSQLiteDatabase::class.java.classLoader,
                arrayOf(SupportSQLiteDatabase::class.java),
            ) { _, method, args ->
                if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                    executedStatements.add(args[0] as String)
                }
                null
            } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_1_2.migrate(fakeDatabase)

        val expectedStatements =
            listOf(
                "ALTER TABLE animes ADD COLUMN genres TEXT",
                "ALTER TABLE animes ADD COLUMN studios TEXT",
                "ALTER TABLE animes ADD COLUMN source TEXT",
                "ALTER TABLE animes ADD COLUMN synopsis TEXT",
                "ALTER TABLE animes ADD COLUMN rating TEXT",
                "ALTER TABLE animes ADD COLUMN rank INTEGER",
                "ALTER TABLE animes ADD COLUMN popularity INTEGER",
                "ALTER TABLE animes ADD COLUMN num_list_users INTEGER",
                "ALTER TABLE animes ADD COLUMN average_episode_duration INTEGER",
                "ALTER TABLE animes ADD COLUMN nsfw TEXT",
            )

        assertEquals(expectedStatements, executedStatements)
    }

    @Test
    fun `migration 2 to 3 has correct version boundaries`() {
        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)
    }

    @Test
    fun `migration 2 to 3 creates recommendations and candidates tables and indices`() {
        val executedStatements = mutableListOf<String>()

        val fakeDatabase =
            Proxy.newProxyInstance(
                SupportSQLiteDatabase::class.java.classLoader,
                arrayOf(SupportSQLiteDatabase::class.java),
            ) { _, method, args ->
                if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                    executedStatements.add(args[0] as String)
                }
                null
            } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_2_3.migrate(fakeDatabase)

        assertEquals(6, executedStatements.size)
        assertTrue(executedStatements[0].contains("CREATE TABLE IF NOT EXISTS `recommendations`"))
        assertTrue(executedStatements[1].contains("index_recommendations_anime_id"))
        assertTrue(executedStatements[2].contains("index_recommendations_genre_rank"))
        assertTrue(executedStatements[3].contains("index_recommendations_theme_rank"))
        assertTrue(
            executedStatements[4].contains(
                "CREATE TABLE IF NOT EXISTS `recommendation_candidates`",
            ),
        )
        assertTrue(executedStatements[5].contains("index_recommendation_candidates_anime_id"))
    }

    @Test
    fun `migration 3 to 4 has correct version boundaries`() {
        assertEquals(3, AppDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, AppDatabase.MIGRATION_3_4.endVersion)
    }

    @Test
    fun `migration 3 to 4 creates new_season_animes table and indices`() {
        val executedStatements = mutableListOf<String>()

        val fakeDatabase =
            Proxy.newProxyInstance(
                SupportSQLiteDatabase::class.java.classLoader,
                arrayOf(SupportSQLiteDatabase::class.java),
            ) { _, method, args ->
                if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                    executedStatements.add(args[0] as String)
                }
                null
            } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_3_4.migrate(fakeDatabase)

        assertEquals(3, executedStatements.size)
        assertTrue(executedStatements[0].contains("CREATE TABLE IF NOT EXISTS `new_season_animes`"))
        assertTrue(executedStatements[0].contains("FOREIGN KEY(`anime_id`) REFERENCES `animes`(`id`)"))
        assertTrue(
            executedStatements[0].contains("FOREIGN KEY(`parent_anime_id`) REFERENCES `animes`(`id`)"),
        )
        assertTrue(executedStatements[1].contains("index_new_season_animes_anime_id"))
        assertTrue(executedStatements[2].contains("index_new_season_animes_parent_anime_id"))
    }

    @Test
    fun `migration 4 to 5 has correct version boundaries`() {
        assertEquals(4, AppDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, AppDatabase.MIGRATION_4_5.endVersion)
    }

    @Test
    fun `migration 4 to 5 recreates recommendations and candidates with NO ACTION`() {
        val executed = executeMigration45()
        assertEquals(18, executed.size)

        // Recommendations table recreation
        assertTrue(executed[0].contains("CREATE TABLE IF NOT EXISTS `recommendations_new`"))
        assertTrue(executed[0].contains("ON UPDATE NO ACTION ON DELETE NO ACTION"))
        assertTrue(
            executed[1].contains(
                "INSERT INTO `recommendations_new` SELECT * FROM `recommendations`",
            ),
        )
        assertTrue(executed[2].contains("DROP TABLE `recommendations`"))
        assertTrue(
            executed[3].contains(
                "ALTER TABLE `recommendations_new` RENAME TO `recommendations`",
            ),
        )

        // Recommendation candidates table recreation
        assertTrue(
            executed[7].contains(
                "CREATE TABLE IF NOT EXISTS `recommendation_candidates_new`",
            ),
        )
        assertTrue(executed[7].contains("ON UPDATE NO ACTION ON DELETE NO ACTION"))
        assertTrue(
            executed[8].contains(
                "INSERT INTO `recommendation_candidates_new` SELECT * FROM `recommendation_candidates`",
            ),
        )
        assertTrue(executed[9].contains("DROP TABLE `recommendation_candidates`"))
        assertTrue(
            executed[10].contains(
                "ALTER TABLE `recommendation_candidates_new` RENAME TO `recommendation_candidates`",
            ),
        )
    }

    @Test
    fun `migration 4 to 5 recreates new season animes with NO ACTION`() {
        val executed = executeMigration45()

        assertTrue(executed[12].contains("CREATE TABLE IF NOT EXISTS `new_season_animes_new`"))
        assertTrue(executed[12].contains("FOREIGN KEY(`anime_id`) REFERENCES `animes`(`id`)"))
        assertTrue(
            executed[12].contains(
                "FOREIGN KEY(`parent_anime_id`) REFERENCES `animes`(`id`)",
            ),
        )
        assertTrue(executed[12].contains("ON UPDATE NO ACTION ON DELETE NO ACTION"))
        assertTrue(
            executed[13].contains(
                "INSERT INTO `new_season_animes_new` SELECT * FROM `new_season_animes`",
            ),
        )
        assertTrue(executed[14].contains("DROP TABLE `new_season_animes`"))
        assertTrue(
            executed[15].contains(
                "ALTER TABLE `new_season_animes_new` RENAME TO `new_season_animes`",
            ),
        )
        assertTrue(executed[16].contains("index_new_season_animes_anime_id"))
        assertTrue(executed[17].contains("index_new_season_animes_parent_anime_id"))
    }

    private fun executeMigration45(): List<String> {
        val executedStatements = mutableListOf<String>()
        val fakeDatabase =
            Proxy.newProxyInstance(
                SupportSQLiteDatabase::class.java.classLoader,
                arrayOf(SupportSQLiteDatabase::class.java),
            ) { _, method, args ->
                if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                    executedStatements.add(args[0] as String)
                }
                null
            } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_4_5.migrate(fakeDatabase)
        return executedStatements
    }
}
