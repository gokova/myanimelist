package com.gokova.myanimelist.core.datastore.di

import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.core.datastore.AuthPreferencesImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {
    @Binds
    abstract fun bindAuthPreferences(authPreferencesImpl: AuthPreferencesImpl): AuthPreferences
}
