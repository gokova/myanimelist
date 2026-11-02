package com.gokova.myanimelist.feature.search.data.repository

import com.gokova.myanimelist.core.ai.client.AiSearchClient
import com.gokova.myanimelist.core.ai.model.AiException
import com.gokova.myanimelist.core.datastore.AiPreferences
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.network.di.IoDispatcher
import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import com.gokova.myanimelist.feature.search.data.local.SearchLocalDataSource
import com.gokova.myanimelist.feature.search.data.mapper.AiCandidateResolver
import com.gokova.myanimelist.feature.search.data.mapper.SearchMapper
import com.gokova.myanimelist.feature.search.data.remote.SearchRemoteDataSource
import com.gokova.myanimelist.feature.search.domain.model.AiSearchResult
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem
import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage
import com.gokova.myanimelist.feature.search.domain.repository.SearchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

private val QUERY_PARAM_REGEX = Regex("""([?&]q=)[^&]+""")
private const val CANDIDATE_LOOKUP_PACING_MS = 250L

@Singleton
class SearchRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: SearchRemoteDataSource,
        private val localDataSource: SearchLocalDataSource,
        private val candidateResolver: AiCandidateResolver,
        private val aiPreferences: AiPreferences,
        private val aiSearchClient: AiSearchClient,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : SearchRepository {
        override suspend fun searchAnime(query: String): Result<SearchResultPage> =
            withContext(ioDispatcher) {
                try {
                    val userList = localDataSource.getUserAnimeList()
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
                    val userList = localDataSource.getUserAnimeList()
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

        override suspend fun aiSearchAnime(prompt: String): Result<AiSearchResult> =
            withContext(ioDispatcher) {
                try {
                    val apiKey = aiPreferences.geminiApiKey.first()
                    if (apiKey.isNullOrBlank()) {
                        return@withContext Result.failure(AiException.MissingApiKey())
                    }

                    val aiResult = aiSearchClient.searchCandidates(prompt, apiKey)
                    val interpretation =
                        aiResult.getOrElse { return@withContext Result.failure(it) }

                    val userList = localDataSource.getUserAnimeList()
                    val ignoredIds = localDataSource.getIgnoredAnimeIds()
                    val (resolvedItems, unverifiedTitles) =
                        resolveCandidates(interpretation.candidates, userList, ignoredIds)

                    Result.success(
                        AiSearchResult(
                            intentSummary = interpretation.intentSummary,
                            explanation = interpretation.explanation,
                            items = resolvedItems,
                            unverifiedCandidateTitles = unverifiedTitles,
                        ),
                    )
                } catch (ce: CancellationException) {
                    throw ce
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.domain.e(e) { "Failed to perform AI-assisted search" }
                    Result.failure(e)
                }
            }

        private suspend fun resolveCandidates(
            candidates: List<com.gokova.myanimelist.core.ai.model.AiCandidateHint>,
            userList: List<com.gokova.myanimelist.core.database.model.UserAnimeListItem>,
            ignoredIds: Set<Long>,
        ): Pair<List<SearchAnimeItem>, List<String>> {
            val resolvedItems = mutableListOf<SearchAnimeItem>()
            val resolvedIds = mutableSetOf<Long>()
            val unverifiedTitles = mutableListOf<String>()

            for ((index, candidate) in candidates.withIndex()) {
                if (index > 0) {
                    delay(CANDIDATE_LOOKUP_PACING_MS.milliseconds)
                }
                try {
                    val response = remoteDataSource.searchAnime(candidate.title)
                    val resolved =
                        candidateResolver.resolveCandidate(
                            candidate = candidate,
                            entries = response.data,
                            userList = userList,
                            ignoredIds = ignoredIds,
                            alreadyResolvedIds = resolvedIds,
                        )
                    if (resolved != null) {
                        resolvedItems.add(resolved)
                        resolvedIds.add(resolved.id)
                    } else {
                        unverifiedTitles.add(candidate.title)
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.network.w(e) {
                        "Failed to resolve AI candidate: ${candidate.title}"
                    }
                    unverifiedTitles.add(candidate.title)
                }
            }
            return resolvedItems to unverifiedTitles
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
            userList: List<com.gokova.myanimelist.core.database.model.UserAnimeListItem>,
        ): SearchResultPage {
            val userStatusMap = userList.associate { it.userAnime.animeId to it.userAnime.status }
            val items =
                response.data
                    .distinctBy { it.node.id }
                    .map { entry ->
                        val localStatus = userStatusMap[entry.node.id]
                        val candidateGenres = entry.node.genres?.map { it.name } ?: emptyList()
                        val matchPercent =
                            candidateResolver.calculateTasteMatch(
                                genres = candidateGenres,
                                meanScore = entry.node.mean,
                                numListUsers = entry.node.numListUsers,
                                userList = userList,
                            )
                        SearchMapper.toDomain(entry, localStatus, matchPercent)
                    }
            return SearchResultPage(items = items, nextUrl = response.paging?.next)
        }
    }
