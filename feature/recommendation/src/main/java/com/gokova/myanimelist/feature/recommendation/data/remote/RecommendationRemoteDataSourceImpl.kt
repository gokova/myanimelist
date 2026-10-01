package com.gokova.myanimelist.feature.recommendation.data.remote

import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.util.executeWithRetry
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

        companion object {
            private val PAGE_REQUEST_DELAY: Duration = 500.milliseconds
        }
    }
