package com.gokova.myanimelist.feature.recommendation.di

import com.gokova.myanimelist.feature.recommendation.data.remote.RecommendationRemoteDataSource
import com.gokova.myanimelist.feature.recommendation.data.remote.RecommendationRemoteDataSourceImpl
import com.gokova.myanimelist.feature.recommendation.data.remote.SeasonalPeriodCalculator
import com.gokova.myanimelist.feature.recommendation.data.repository.NewSeasonRepositoryImpl
import com.gokova.myanimelist.feature.recommendation.data.repository.RecommendationRepositoryImpl
import com.gokova.myanimelist.feature.recommendation.data.work.NewSeasonScheduler
import com.gokova.myanimelist.feature.recommendation.data.work.NewSeasonSchedulerImpl
import com.gokova.myanimelist.feature.recommendation.data.work.NewSeasonSyncTracker
import com.gokova.myanimelist.feature.recommendation.data.work.NewSeasonSyncTrackerImpl
import com.gokova.myanimelist.feature.recommendation.data.work.RecommendationScheduler
import com.gokova.myanimelist.feature.recommendation.data.work.RecommendationSchedulerImpl
import com.gokova.myanimelist.feature.recommendation.domain.repository.NewSeasonRepository
import com.gokova.myanimelist.feature.recommendation.domain.repository.RecommendationRepository
import com.gokova.myanimelist.feature.recommendation.presentation.BackgroundSyncPermissionManager
import com.gokova.myanimelist.feature.recommendation.presentation.BackgroundSyncPermissionManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecommendationModule {
    @Binds
    @Singleton
    abstract fun bindRecommendationRemoteDataSource(
        impl: RecommendationRemoteDataSourceImpl,
    ): RecommendationRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(
        impl: RecommendationRepositoryImpl,
    ): RecommendationRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationScheduler(
        impl: RecommendationSchedulerImpl,
    ): RecommendationScheduler

    @Binds
    @Singleton
    abstract fun bindNewSeasonRepository(impl: NewSeasonRepositoryImpl): NewSeasonRepository

    @Binds
    @Singleton
    abstract fun bindNewSeasonScheduler(impl: NewSeasonSchedulerImpl): NewSeasonScheduler

    @Binds
    @Singleton
    abstract fun bindNewSeasonSyncTracker(impl: NewSeasonSyncTrackerImpl): NewSeasonSyncTracker

    @Binds
    @Singleton
    abstract fun bindBackgroundSyncPermissionManager(
        impl: BackgroundSyncPermissionManagerImpl,
    ): BackgroundSyncPermissionManager

    companion object {
        @Provides
        @Singleton
        fun provideSeasonalPeriodCalculator(): SeasonalPeriodCalculator = SeasonalPeriodCalculator()
    }
}
