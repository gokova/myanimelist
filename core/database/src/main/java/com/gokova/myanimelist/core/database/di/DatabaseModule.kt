package com.gokova.myanimelist.core.database.di

import android.content.Context
import androidx.room.Room
import com.gokova.myanimelist.core.database.AppDatabase
import com.gokova.myanimelist.core.database.dao.RecommendationDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
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
            ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    @Singleton
    fun provideUserAnimeListDao(database: AppDatabase): UserAnimeListDao = database.userAnimeListDao()

    @Provides
    @Singleton
    fun provideRecommendationDao(database: AppDatabase): RecommendationDao = database.recommendationDao()
}
