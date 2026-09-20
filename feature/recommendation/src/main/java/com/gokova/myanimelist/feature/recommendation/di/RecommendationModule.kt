package com.gokova.myanimelist.feature.recommendation.di

import com.gokova.myanimelist.feature.recommendation.data.remote.RecommendationRemoteDataSource
import com.gokova.myanimelist.feature.recommendation.data.remote.RecommendationRemoteDataSourceImpl
import com.gokova.myanimelist.feature.recommendation.data.remote.SeasonalPeriodCalculator
import com.gokova.myanimelist.feature.recommendation.data.repository.RecommendationRepositoryImpl
import com.gokova.myanimelist.feature.recommendation.data.work.RecommendationScheduler
import com.gokova.myanimelist.feature.recommendation.data.work.RecommendationSchedulerImpl
import com.gokova.myanimelist.feature.recommendation.domain.repository.RecommendationRepository
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

    companion object {
        @Provides
        @Singleton
        fun provideSeasonalPeriodCalculator(): SeasonalPeriodCalculator = SeasonalPeriodCalculator()
    }
}
