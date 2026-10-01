package com.gokova.myanimelist.feature.mylist.data.remote

import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.core.network.util.executeWithRetry
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

                val currentUrl = nextUrl
                val response =
                    executeWithRetry {
                        if (currentUrl.isNullOrBlank()) {
                            apiService.getUserAnimeList(
                                limit = MalApiService.DEFAULT_PAGE_LIMIT,
                                offset = 0,
                            )
                        } else {
                            apiService.getAnimeListNextPage(currentUrl)
                        }
                    }
                entries.addAll(response.data)

                nextUrl = response.paging?.next
                if (response.data.isEmpty()) {
                    break
                }
                isFirstPage = false
            }
            return entries
        }

        companion object {
            private val PAGE_REQUEST_DELAY: Duration = 500.milliseconds
        }
    }
