package com.gokova.myanimelist.feature.taste.di

import com.gokova.myanimelist.feature.taste.data.repository.TasteRepositoryImpl
import com.gokova.myanimelist.feature.taste.domain.repository.TasteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TasteModule {
    @Binds
    @Singleton
    abstract fun bindTasteRepository(impl: TasteRepositoryImpl): TasteRepository
}
