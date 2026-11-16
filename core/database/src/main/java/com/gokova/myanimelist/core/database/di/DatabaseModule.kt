package com.gokova.myanimelist.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.gokova.myanimelist.core.database.AppDatabase
import com.gokova.myanimelist.core.database.dao.AnimeRelationshipDao
import com.gokova.myanimelist.core.database.dao.IgnoredAnimeDao
import com.gokova.myanimelist.core.database.dao.NewSeasonDao
import com.gokova.myanimelist.core.database.dao.PendingUserAnimeMutationDao
import com.gokova.myanimelist.core.database.dao.RecommendationDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.repository.IgnoredAnimeRepositoryImpl
import com.gokova.myanimelist.core.database.util.DatabaseTransactionRunner
import com.gokova.myanimelist.core.domain.repository.IgnoredAnimeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                "mal_database",
            ).addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
            ).build()

    @Provides
    @Singleton
    fun provideUserAnimeListDao(database: AppDatabase): UserAnimeListDao =
        database.userAnimeListDao()

    @Provides
    @Singleton
    fun providePendingUserAnimeMutationDao(database: AppDatabase): PendingUserAnimeMutationDao =
        database.pendingUserAnimeMutationDao()

    @Provides
    @Singleton
    fun provideRecommendationDao(database: AppDatabase): RecommendationDao =
        database.recommendationDao()

    @Provides
    @Singleton
    fun provideNewSeasonDao(database: AppDatabase): NewSeasonDao = database.newSeasonDao()

    @Provides
    @Singleton
    fun provideIgnoredAnimeDao(database: AppDatabase): IgnoredAnimeDao = database.ignoredAnimeDao()

    @Provides
    @Singleton
    fun provideAnimeRelationshipDao(database: AppDatabase): AnimeRelationshipDao =
        database.animeRelationshipDao()

    @Provides
    @Singleton
    fun provideIgnoredAnimeRepository(ignoredAnimeDao: IgnoredAnimeDao): IgnoredAnimeRepository =
        IgnoredAnimeRepositoryImpl(ignoredAnimeDao)

    @Provides
    @Singleton
    fun provideDatabaseTransactionRunner(database: AppDatabase): DatabaseTransactionRunner =
        object : DatabaseTransactionRunner {
            override suspend fun <T> invoke(block: suspend () -> T): T =
                database.withTransaction(block)
        }
}
