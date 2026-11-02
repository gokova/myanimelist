package com.gokova.myanimelist.core.ai.di

import com.gokova.myanimelist.core.ai.BuildConfig
import com.gokova.myanimelist.core.ai.client.AiSearchClient
import com.gokova.myanimelist.core.ai.client.GeminiAiClient
import com.gokova.myanimelist.core.ai.remote.GeminiAiService
import com.gokova.myanimelist.core.domain.logging.AppLog
import dagger.Binds
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
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    @Singleton
    abstract fun bindAiSearchClient(client: GeminiAiClient): AiSearchClient

    companion object {
        private const val TIMEOUT_SECONDS = 30L

        @Provides
        @Singleton
        @AiHttpClient
        fun provideAiOkHttpClient(): OkHttpClient {
            val loggingInterceptor =
                HttpLoggingInterceptor { message ->
                    AppLog.network.d { message }
                }.apply {
                    redactHeader("x-goog-api-key")
                    level =
                        if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
                }

            return OkHttpClient
                .Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()
        }

        @Provides
        @Singleton
        fun provideGeminiAiService(
            @AiHttpClient okHttpClient: OkHttpClient,
        ): GeminiAiService {
            val json =
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                }
            val contentType = "application/json".toMediaType()
            return Retrofit
                .Builder()
                .baseUrl(GeminiAiService.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(GeminiAiService::class.java)
        }
    }
}
