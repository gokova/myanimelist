package com.gokova.myanimelist.feature.auth.di

import com.gokova.myanimelist.feature.auth.data.MalOAuthClient
import com.gokova.myanimelist.feature.auth.domain.authenticator.MalAuthenticator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindMalAuthenticator(malOAuthClient: MalOAuthClient): MalAuthenticator
}
