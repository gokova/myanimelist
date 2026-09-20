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
}
