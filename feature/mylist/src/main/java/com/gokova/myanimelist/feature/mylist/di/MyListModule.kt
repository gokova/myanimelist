package com.gokova.myanimelist.feature.mylist.di

import com.gokova.myanimelist.feature.mylist.data.remote.AnimeListRemoteDataSource
import com.gokova.myanimelist.feature.mylist.data.remote.AnimeListRemoteDataSourceImpl
import com.gokova.myanimelist.feature.mylist.data.repository.MyListRepositoryImpl
import com.gokova.myanimelist.feature.mylist.domain.repository.MyListRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MyListModule {
    @Binds
    @Singleton
    abstract fun bindMyListRepository(impl: MyListRepositoryImpl): MyListRepository

    @Binds
    @Singleton
    abstract fun bindAnimeListRemoteDataSource(impl: AnimeListRemoteDataSourceImpl): AnimeListRemoteDataSource
}
