package com.gokova.myanimelist.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.core.datastore.AuthPreferencesImpl
import com.gokova.myanimelist.core.datastore.SyncPreferences
import com.gokova.myanimelist.core.datastore.SyncPreferencesImpl
import com.gokova.myanimelist.core.datastore.UserPreferences
import com.gokova.myanimelist.core.datastore.UserPreferencesImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionDataStore

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_prefs",
)

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {
    @Binds
    abstract fun bindAuthPreferences(authPreferencesImpl: AuthPreferencesImpl): AuthPreferences

    @Binds
    abstract fun bindSyncPreferences(syncPreferencesImpl: SyncPreferencesImpl): SyncPreferences

    @Binds
    abstract fun bindUserPreferences(userPreferencesImpl: UserPreferencesImpl): UserPreferences

    companion object {
        @Provides
        @Singleton
        @SessionDataStore
        fun provideSessionDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.sessionDataStore
    }
}
