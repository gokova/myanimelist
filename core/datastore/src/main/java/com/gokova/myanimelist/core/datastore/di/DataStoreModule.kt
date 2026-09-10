package com.gokova.myanimelist.core.datastore.di

import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.core.datastore.AuthPreferencesImpl
import com.gokova.myanimelist.core.datastore.SyncPreferences
import com.gokova.myanimelist.core.datastore.SyncPreferencesImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {
    @Binds
    abstract fun bindAuthPreferences(authPreferencesImpl: AuthPreferencesImpl): AuthPreferences

    @Binds
    abstract fun bindSyncPreferences(syncPreferencesImpl: SyncPreferencesImpl): SyncPreferences
}
