package com.gokova.myanimelist.feature.franchise.di

import com.gokova.myanimelist.feature.franchise.data.repository.FranchiseRepositoryImpl
import com.gokova.myanimelist.feature.franchise.domain.repository.FranchiseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FranchiseModule {
    @Binds
    @Singleton
    abstract fun bindFranchiseRepository(impl: FranchiseRepositoryImpl): FranchiseRepository
}
