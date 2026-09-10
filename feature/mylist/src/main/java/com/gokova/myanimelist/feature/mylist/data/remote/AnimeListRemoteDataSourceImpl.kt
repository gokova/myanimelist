package com.gokova.myanimelist.feature.mylist.data.remote

import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AnimeListRemoteDataSourceImpl
    @Inject
    constructor(
        private val apiService: MalApiService,
    ) : AnimeListRemoteDataSource {
        override suspend fun fetchAllUserAnime(): List<AnimeListEntryDto> {
            val entries = mutableListOf<AnimeListEntryDto>()
            var nextUrl: String? = null
            var isFirstPage = true

            while (isFirstPage || !nextUrl.isNullOrBlank()) {
                if (!isFirstPage) {
                    delay(PAGE_REQUEST_DELAY)
                }

                val response = fetchPageWithRetry(nextUrl)
                entries.addAll(response.data)

                nextUrl = response.paging?.next
                if (response.data.isEmpty()) {
                    break
                }
                isFirstPage = false
            }
            return entries
        }

        private suspend fun fetchPageWithRetry(nextUrl: String?): AnimeListResponseDto {
            var currentAttempt = 0
            var delayDuration = INITIAL_BACKOFF_DELAY

            while (true) {
                try {
                    return if (nextUrl.isNullOrBlank()) {
                        apiService.getUserAnimeList(
                            limit = MalApiService.DEFAULT_PAGE_LIMIT,
                            offset = 0,
                        )
                    } else {
                        apiService.getUserAnimeListNextPage(nextUrl)
                    }
                } catch (e: IOException) {
                    currentAttempt++
                    if (currentAttempt >= MAX_RETRY_ATTEMPTS) {
                        throw e
                    }
                    delay(delayDuration)
                    delayDuration *= BACKOFF_MULTIPLIER
                } catch (e: HttpException) {
                    val isThrottledOrServerError =
                        e.code() == HTTP_TOO_MANY_REQUESTS ||
                            e.code() in HTTP_SERVER_ERROR_RANGE
                    currentAttempt++
                    if (!isThrottledOrServerError || currentAttempt >= MAX_RETRY_ATTEMPTS) {
                        throw e
                    }
                    delay(delayDuration)
                    delayDuration *= BACKOFF_MULTIPLIER
                }
            }
        }

        companion object {
            private const val HTTP_TOO_MANY_REQUESTS = 429
            private val HTTP_SERVER_ERROR_RANGE = 500..599
            private val PAGE_REQUEST_DELAY: Duration = 500.milliseconds
            private val INITIAL_BACKOFF_DELAY: Duration = 1.seconds
            private const val BACKOFF_MULTIPLIER = 2
            private const val MAX_RETRY_ATTEMPTS = 3
        }
    }
