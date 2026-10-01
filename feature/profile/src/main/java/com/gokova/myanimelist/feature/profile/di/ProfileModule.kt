package com.gokova.myanimelist.feature.profile.di

import com.gokova.myanimelist.core.domain.repository.ProfileRepository
import com.gokova.myanimelist.core.domain.usecase.ObserveUserProfileUseCase
import com.gokova.myanimelist.core.domain.usecase.RefreshUserProfileUseCase
import com.gokova.myanimelist.feature.profile.data.repository.ProfileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {
    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl,
    ): ProfileRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ProfileUseCaseModule {
    @Provides
    @Singleton
    fun provideObserveUserProfileUseCase(
        profileRepository: ProfileRepository,
    ): ObserveUserProfileUseCase = ObserveUserProfileUseCase(profileRepository)

    @Provides
    @Singleton
    fun provideRefreshUserProfileUseCase(
        profileRepository: ProfileRepository,
    ): RefreshUserProfileUseCase = RefreshUserProfileUseCase(profileRepository)
}
