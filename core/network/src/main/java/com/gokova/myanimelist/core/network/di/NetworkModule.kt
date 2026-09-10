package com.gokova.myanimelist.core.network.di

import com.gokova.myanimelist.core.network.BuildConfig
import com.gokova.myanimelist.core.network.auth.AuthInterceptor
import com.gokova.myanimelist.core.network.auth.TokenAuthenticator
import com.gokova.myanimelist.core.network.config.OAuthConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOAuthConfig(): OAuthConfig =
        OAuthConfig(
            clientId = BuildConfig.MAL_CLIENT_ID,
        )

    @Provides
    @Singleton
    @Unauthenticated
    fun provideUnauthenticatedOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    @Authenticated
    fun provideAuthenticatedOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()

    @Provides
    @Singleton
    fun provideDefaultOkHttpClient(
        @Authenticated okHttpClient: OkHttpClient,
    ): OkHttpClient = okHttpClient
}
