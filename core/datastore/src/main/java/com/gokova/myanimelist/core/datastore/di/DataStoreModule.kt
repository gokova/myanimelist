package com.gokova.myanimelist.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.gokova.myanimelist.core.datastore.AiPreferences
import com.gokova.myanimelist.core.datastore.AiPreferencesImpl
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

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiDataStore

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_prefs",
)

private val Context.aiDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_prefs",
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

    @Binds
    abstract fun bindAiPreferences(aiPreferencesImpl: AiPreferencesImpl): AiPreferences

    companion object {
        @Provides
        @Singleton
        @SessionDataStore
        fun provideSessionDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.sessionDataStore

        @Provides
        @Singleton
        @AiDataStore
        fun provideAiDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.aiDataStore
    }
}
