package com.gokova.myanimelist.core.network.di

import com.gokova.myanimelist.core.network.BuildConfig
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.auth.AuthInterceptor
import com.gokova.myanimelist.core.network.auth.TokenAuthenticator
import com.gokova.myanimelist.core.network.config.OAuthConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
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
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            redactHeader("Cookie")
            level =
                if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
        }

    @Provides
    @Singleton
    @Unauthenticated
    fun provideUnauthenticatedLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            redactHeader("Cookie")
            level =
                if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
        }

    @Provides
    @Singleton
    @Unauthenticated
    fun provideUnauthenticatedOkHttpClient(
        @Unauthenticated logging: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(logging)
            .build()

    @Provides
    @Singleton
    @Authenticated
    fun provideAuthenticatedOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .build()

    @Provides
    @Singleton
    fun provideDefaultOkHttpClient(
        @Authenticated okHttpClient: OkHttpClient,
    ): OkHttpClient = okHttpClient

    @Provides
    @Singleton
    fun provideNetworkJson(): Json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    @Provides
    @Singleton
    fun provideRetrofit(
        @Authenticated okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://api.myanimelist.net/")
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType()),
            ).build()

    @Provides
    @Singleton
    fun provideMalApiService(retrofit: Retrofit): MalApiService = retrofit.create(MalApiService::class.java)
}
