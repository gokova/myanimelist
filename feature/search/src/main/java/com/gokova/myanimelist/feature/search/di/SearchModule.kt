package com.gokova.myanimelist.feature.search.di

import com.gokova.myanimelist.feature.search.data.remote.SearchRemoteDataSource
import com.gokova.myanimelist.feature.search.data.remote.SearchRemoteDataSourceImpl
import com.gokova.myanimelist.feature.search.data.repository.SearchRepositoryImpl
import com.gokova.myanimelist.feature.search.domain.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {
    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindSearchRemoteDataSource(
        impl: SearchRemoteDataSourceImpl,
    ): SearchRemoteDataSource
}
