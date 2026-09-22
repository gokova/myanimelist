package com.gokova.myanimelist.feature.recommendation.data.remote

import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RecommendationRemoteDataSourceImpl
    @Inject
    constructor(
        private val apiService: MalApiService,
    ) : RecommendationRemoteDataSource {
        override suspend fun fetchTopRankingAnime(limit: Int): List<AnimeNodeDto> {
            val nodes = mutableListOf<AnimeNodeDto>()
            var nextUrl: String? = null
            var isFirstPage = true

            while (isFirstPage || (!nextUrl.isNullOrBlank() && nodes.size < limit)) {
                if (!isFirstPage) {
                    delay(PAGE_REQUEST_DELAY)
                }

                val targetLimit = (limit - nodes.size).coerceIn(1, MalApiService.DEFAULT_PAGE_LIMIT)
                val response =
                    executeWithRetry {
                        if (isFirstPage) {
                            apiService.getAnimeRanking(
                                rankingType = "all",
                                limit = targetLimit,
                                offset = 0,
                            )
                        } else {
                            apiService.getAnimeListNextPage(nextUrl!!)
                        }
                    }

                nodes.addAll(response.data.map { it.node })
                nextUrl = response.paging?.next
                if (response.data.isEmpty()) {
                    break
                }
                isFirstPage = false
            }

            return nodes.take(limit)
        }

        override suspend fun fetchSeasonalAnime(
            year: Int,
            season: String,
            limit: Int,
        ): List<AnimeNodeDto> {
            val nodes = mutableListOf<AnimeNodeDto>()
            var nextUrl: String? = null
            var isFirstPage = true

            while (isFirstPage || (!nextUrl.isNullOrBlank() && nodes.size < limit)) {
                if (!isFirstPage) {
                    delay(PAGE_REQUEST_DELAY)
                }

                val targetLimit = (limit - nodes.size).coerceIn(1, limit)
                val response =
                    executeWithRetry {
                        if (isFirstPage) {
                            apiService.getSeasonalAnime(
                                year = year,
                                season = season,
                                limit = targetLimit,
                                offset = 0,
                            )
                        } else {
                            apiService.getAnimeListNextPage(nextUrl!!)
                        }
                    }

                nodes.addAll(response.data.map { it.node })
                nextUrl = response.paging?.next
                if (response.data.isEmpty()) {
                    break
                }
                isFirstPage = false
            }

            return nodes.take(limit)
        }

        // TODO: Extract duplicate retry logic into a shared helper in :core:network
        private suspend fun executeWithRetry(
            block: suspend () -> AnimeListResponseDto,
        ): AnimeListResponseDto {
            var currentAttempt = 0
            var delayDuration = INITIAL_BACKOFF_DELAY

            while (true) {
                try {
                    return block()
                } catch (e: IOException) {
                    currentAttempt++
                    if (currentAttempt >= MAX_RETRY_ATTEMPTS) {
                        throw e
                    }
                    delay(delayDuration)
                    delayDuration *= BACKOFF_MULTIPLIER
                } catch (e: HttpException) {
                    val isRetryable =
                        e.code() == HTTP_TOO_MANY_REQUESTS ||
                            e.code() in HTTP_SERVER_ERROR_RANGE
                    currentAttempt++
                    if (!isRetryable || currentAttempt >= MAX_RETRY_ATTEMPTS) {
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
