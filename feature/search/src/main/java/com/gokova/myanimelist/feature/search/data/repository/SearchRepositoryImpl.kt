package com.gokova.myanimelist.feature.search.data.repository

import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.network.di.IoDispatcher
import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import com.gokova.myanimelist.feature.search.data.mapper.SearchMapper
import com.gokova.myanimelist.feature.search.data.remote.SearchRemoteDataSource
import com.gokova.myanimelist.feature.search.domain.algorithm.SearchTasteMatcher
import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage
import com.gokova.myanimelist.feature.search.domain.repository.SearchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val QUERY_PARAM_REGEX = Regex("""([?&]q=)[^&]+""")

@Singleton
class SearchRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: SearchRemoteDataSource,
        private val userAnimeListDao: UserAnimeListDao,
        private val tasteMatcher: SearchTasteMatcher,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : SearchRepository {
        override suspend fun searchAnime(query: String): Result<SearchResultPage> =
            withContext(ioDispatcher) {
                try {
                    val userList = userAnimeListDao.getAllUserAnime()
                    val response = remoteDataSource.searchAnime(query)
                    val page = mapResponse(response, userList)
                    AppLog.domain.i { "Search request succeeded with ${page.items.size} results" }
                    Result.success(page)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.network.e(sanitizeException(e)) { "Failed to search anime" }
                    Result.failure(e)
                }
            }

        override suspend fun loadMore(nextUrl: String): Result<SearchResultPage> =
            withContext(ioDispatcher) {
                try {
                    val userList = userAnimeListDao.getAllUserAnime()
                    val response = remoteDataSource.fetchNextPage(nextUrl)
                    val page = mapResponse(response, userList)
                    AppLog.domain.i { "Loaded next page with ${page.items.size} results" }
                    Result.success(page)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.network.e(sanitizeException(e)) {
                        "Failed to load more search results"
                    }
                    Result.failure(e)
                }
            }

        private fun sanitizeException(e: Exception): Exception {
            val message = e.message ?: return e
            val sanitized = message.replace(QUERY_PARAM_REGEX, "$1<redacted>")
            return if (sanitized != message) {
                Exception(sanitized, e.cause)
            } else {
                e
            }
        }

        private fun mapResponse(
            response: AnimeListResponseDto,
            userList: List<UserAnimeListItem>,
        ): SearchResultPage {
            val userStatusMap = userList.associate { it.userAnime.animeId to it.userAnime.status }
            val items =
                response.data
                    .distinctBy { it.node.id }
                    .map { entry ->
                        val localStatus = userStatusMap[entry.node.id]
                        val candidateGenres = entry.node.genres?.map { it.name } ?: emptyList()
                        val matchPercent =
                            tasteMatcher.calculateMatch(
                                candidateGenres = candidateGenres,
                                candidateMeanScore = entry.node.mean,
                                candidateNumListUsers = entry.node.numListUsers,
                                userList = userList,
                            )
                        SearchMapper.toDomain(entry, localStatus, matchPercent)
                    }
            return SearchResultPage(items = items, nextUrl = response.paging?.next)
        }
    }
