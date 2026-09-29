package com.gokova.myanimelist.feature.details.di

import com.gokova.myanimelist.feature.details.data.repository.AnimeDetailsRepositoryImpl
import com.gokova.myanimelist.feature.details.domain.repository.AnimeDetailsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DetailsModule {
    @Binds
    @Singleton
    abstract fun bindAnimeDetailsRepository(
        impl: AnimeDetailsRepositoryImpl,
    ): AnimeDetailsRepository
}
