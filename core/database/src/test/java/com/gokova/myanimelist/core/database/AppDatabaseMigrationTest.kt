package com.gokova.myanimelist.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
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
}
