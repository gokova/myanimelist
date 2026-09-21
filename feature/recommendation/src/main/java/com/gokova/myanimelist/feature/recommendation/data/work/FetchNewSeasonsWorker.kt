package com.gokova.myanimelist.feature.recommendation.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gokova.myanimelist.core.database.dao.NewSeasonDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.NewSeasonAnimeEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeDetailsDto
import com.gokova.myanimelist.feature.recommendation.data.mapper.RecommendationMapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@HiltWorker
class FetchNewSeasonsWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val malApiService: MalApiService,
        private val userAnimeListDao: UserAnimeListDao,
        private val newSeasonDao: NewSeasonDao,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            AppLog.domain.i { "FetchNewSeasonsWorker started" }
            return try {
                val userAnimeList = userAnimeListDao.getAllUserAnime()
                if (userAnimeList.size < MIN_USER_LIST_THRESHOLD) {
                    AppLog.domain.i {
                        "User anime list has ${userAnimeList.size} < " +
                            "$MIN_USER_LIST_THRESHOLD, skipping"
                    }
                    val previousIds = newSeasonDao.getNewSeasonAnimeIds()
                    newSeasonDao.saveNewSeasonsTransaction(
                        animes = emptyList(),
                        newSeasons = emptyList(),
                        discardedAnimeIds = previousIds,
                    )
                    Result.success()
                } else {
                    val hasRetryableErrors = processNewSeasons(userAnimeList)
                    if (hasRetryableErrors) {
                        AppLog.domain.w {
                            "FetchNewSeasonsWorker finished with partial retryable errors"
                        }
                        Result.retry()
                    } else {
                        Result.success()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                AppLog.domain.w(e) { "FetchNewSeasonsWorker failed due to network error" }
                Result.retry()
            } catch (e: HttpException) {
                if (e.code() == HTTP_TOO_MANY_REQUESTS || e.code() in HTTP_SERVER_ERROR_RANGE) {
                    AppLog.domain.w(e) {
                        "FetchNewSeasonsWorker hit retryable error: ${e.code()}"
                    }
                    Result.retry()
                } else {
                    AppLog.domain.e(e) {
                        "FetchNewSeasonsWorker encountered HTTP error: ${e.code()}"
                    }
                    Result.failure()
                }
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                AppLog.domain.e(e) { "FetchNewSeasonsWorker encountered fatal error" }
                Result.failure()
            }
        }

        private suspend fun processNewSeasons(userAnimeList: List<UserAnimeListItem>): Boolean {
            val previousAnimeIds = newSeasonDao.getNewSeasonAnimeIds()
            val userAnimeIds = userAnimeList.map { it.anime.id }.toSet()
            val candidateResult = collectCandidates(userAnimeList, userAnimeIds)

            if (candidateResult.candidateMap.isEmpty()) {
                AppLog.domain.i { "No new season candidates discovered" }
                if (candidateResult.failedParentAnimeIds.isEmpty()) {
                    newSeasonDao.saveNewSeasonsTransaction(
                        animes = emptyList(),
                        newSeasons = emptyList(),
                        discardedAnimeIds = previousAnimeIds,
                    )
                }
                return candidateResult.failedParentAnimeIds.isNotEmpty()
            }

            val fetchResult = fetchCandidateEntities(candidateResult.candidateMap)

            if (fetchResult.savedAnimeIds.isEmpty() &&
                fetchResult.retryableFailedCandidateIds.isNotEmpty()
            ) {
                throw IOException(
                    "Failed to fetch any candidate anime details: all candidates failed",
                )
            }

            pruneObsoleteSeasons(
                previousAnimeIds = previousAnimeIds,
                failedParentAnimeIds = candidateResult.failedParentAnimeIds,
                fetchResult = fetchResult,
            )

            AppLog.domain.i {
                "FetchNewSeasonsWorker finished batch: " +
                    "${fetchResult.savedAnimeIds.size} new seasons saved, " +
                    "${fetchResult.unreachedCandidateIds.size} deferred"
            }

            return candidateResult.failedParentAnimeIds.isNotEmpty() ||
                fetchResult.retryableFailedCandidateIds.isNotEmpty() ||
                fetchResult.unreachedCandidateIds.isNotEmpty()
        }

        private suspend fun pruneObsoleteSeasons(
            previousAnimeIds: List<Long>,
            failedParentAnimeIds: Set<Long>,
            fetchResult: CandidateFetchResult,
        ) {
            val protectedParentChildIds =
                if (failedParentAnimeIds.isNotEmpty()) {
                    newSeasonDao.getNewSeasonAnimeIds(failedParentAnimeIds.toList()).toSet()
                } else {
                    emptySet()
                }

            val discardedIds =
                previousAnimeIds.filter { id ->
                    id !in fetchResult.savedAnimeIds &&
                        id !in protectedParentChildIds &&
                        id !in fetchResult.retryableFailedCandidateIds &&
                        id !in fetchResult.unreachedCandidateIds
                }

            if (discardedIds.isNotEmpty()) {
                newSeasonDao.saveNewSeasonsTransaction(discardedAnimeIds = discardedIds)
            }
        }

        private suspend fun collectCandidates(
            userAnimeList: List<UserAnimeListItem>,
            userAnimeIds: Set<Long>,
        ): CandidateCollectionResult {
            val candidateMap = mutableMapOf<Long, CandidateRelation>()
            val failedParentAnimeIds = mutableSetOf<Long>()
            var successCount = 0

            for (userItem in userAnimeList) {
                if (isStopped) {
                    AppLog.domain.i {
                        "FetchNewSeasonsWorker stopped by system during parent scan"
                    }
                    failedParentAnimeIds.add(userItem.anime.id)
                    continue
                }
                val animeId = userItem.anime.id
                val fetchResult =
                    fetchAnimeDetailsWithRetry(
                        animeId = animeId,
                        fields = MalApiService.FIELDS_RELATED_ANIME,
                        itemType = "User anime",
                    )
                when (fetchResult) {
                    is FetchItemResult.Success -> {
                        successCount++
                        collectNarrativeCandidates(
                            details = fetchResult.details,
                            userAnimeIds = userAnimeIds,
                            parentAnimeId = animeId,
                            candidateMap = candidateMap,
                        )
                    }
                    is FetchItemResult.NotFound -> {
                        AppLog.domain.w { "User anime $animeId returned 404 Not Found" }
                    }
                    is FetchItemResult.RetryableError -> {
                        failedParentAnimeIds.add(animeId)
                    }
                }
                delay(POLITE_DELAY_MS.milliseconds)
            }

            if (userAnimeList.isNotEmpty() &&
                successCount == 0 &&
                failedParentAnimeIds.isNotEmpty()
            ) {
                throw IOException("Failed to fetch related anime for all user anime items")
            }

            return CandidateCollectionResult(candidateMap, failedParentAnimeIds)
        }

        private fun collectNarrativeCandidates(
            details: AnimeDetailsDto,
            userAnimeIds: Set<Long>,
            parentAnimeId: Long,
            candidateMap: MutableMap<Long, CandidateRelation>,
        ) {
            val edges = details.relatedAnime ?: return
            for (edge in edges) {
                val relatedId = edge.node.id
                if (relatedId !in userAnimeIds && edge.relationType in NARRATIVE_RELATIONS) {
                    candidateMap.putIfAbsent(
                        relatedId,
                        CandidateRelation(
                            parentAnimeId = parentAnimeId,
                            relationType = edge.relationType,
                            relationTypeFormatted = edge.relationTypeFormatted,
                        ),
                    )
                }
            }
        }

        private suspend fun fetchCandidateEntities(
            candidateMap: Map<Long, CandidateRelation>,
        ): CandidateFetchResult {
            val existingAnimeIds =
                newSeasonDao.getExistingAnimeIds(candidateMap.keys.toList()).toSet()
            val state = CandidateBatchState(existingAnimeIds)
            var uncachedFetchCount = 0
            val unreachedCandidateIds = mutableSetOf<Long>()

            for ((candidateId, relation) in candidateMap) {
                if (isStopped) {
                    AppLog.domain.i {
                        "FetchNewSeasonsWorker stopped by system, deferring remaining candidates"
                    }
                    unreachedCandidateIds.add(candidateId)
                    continue
                }

                val newSeason =
                    NewSeasonAnimeEntity(
                        animeId = candidateId,
                        parentAnimeId = relation.parentAnimeId,
                        relationType = relation.relationType,
                        relationTypeFormatted = relation.relationTypeFormatted,
                    )
                if (candidateId in existingAnimeIds) {
                    state.addCached(candidateId, newSeason)
                } else if (uncachedFetchCount >= MAX_UNCACHED_FETCHES_PER_RUN) {
                    unreachedCandidateIds.add(candidateId)
                } else {
                    uncachedFetchCount++
                    processCandidate(candidateId, newSeason, state)
                }

                if (state.shouldFlush()) {
                    newSeasonDao.saveNewSeasonsTransaction(
                        animes = state.pendingAnimes.toList(),
                        newSeasons = state.pendingNewSeasons.toList(),
                    )
                    state.clearPending()
                }
            }

            if (state.hasPending()) {
                newSeasonDao.saveNewSeasonsTransaction(
                    animes = state.pendingAnimes.toList(),
                    newSeasons = state.pendingNewSeasons.toList(),
                )
                state.clearPending()
            }

            return CandidateFetchResult(
                savedAnimeIds = state.savedAnimeIds,
                retryableFailedCandidateIds = state.retryableFailedCandidateIds,
                unreachedCandidateIds = unreachedCandidateIds,
            )
        }

        private suspend fun processCandidate(
            candidateId: Long,
            newSeason: NewSeasonAnimeEntity,
            state: CandidateBatchState,
        ) {
            if (candidateId in state.existingAnimeIds) {
                state.addCached(candidateId, newSeason)
                return
            }

            val result =
                fetchAnimeDetailsWithRetry(
                    animeId = candidateId,
                    fields = MalApiService.DEFAULT_ANIME_DETAILS_FIELDS,
                    itemType = "Candidate",
                )
            when (result) {
                is FetchItemResult.Success -> {
                    val anime = RecommendationMapper.toAnimeEntity(result.details.toAnimeNodeDto())
                    state.addSuccess(candidateId, newSeason, anime)
                }
                is FetchItemResult.NotFound -> {
                    AppLog.domain.w { "Candidate $candidateId returned 404 Not Found" }
                }
                is FetchItemResult.RetryableError -> {
                    state.addRetryableFailure(candidateId)
                }
            }
            delay(POLITE_DELAY_MS.milliseconds)
        }

        private suspend fun fetchAnimeDetailsWithRetry(
            animeId: Long,
            fields: String,
            itemType: String,
        ): FetchItemResult {
            var itemResult: FetchItemResult? = null
            var lastError: Exception? = null
            var attempt = 1

            while (attempt <= MAX_ITEM_ATTEMPTS && itemResult == null) {
                when (val attemptResult = executeFetch(animeId, fields, itemType)) {
                    is AttemptResult.Success -> {
                        itemResult = FetchItemResult.Success(attemptResult.details)
                    }
                    is AttemptResult.NotFound -> {
                        itemResult = FetchItemResult.NotFound
                    }
                    is AttemptResult.Retryable -> {
                        lastError = attemptResult.exception
                        delayBeforeRetry(attempt, isRateLimit = attemptResult.isRateLimit)
                        attempt++
                    }
                }
            }

            if (itemResult == null) {
                val error =
                    lastError ?: IOException("Failed to fetch $itemType $animeId details")
                AppLog.domain.w(error) {
                    "Failed to fetch $itemType $animeId details after $MAX_ITEM_ATTEMPTS attempts"
                }
                return FetchItemResult.RetryableError(error)
            }
            return itemResult
        }

        private suspend fun executeFetch(
            animeId: Long,
            fields: String,
            itemType: String,
        ): AttemptResult =
            try {
                val dto = malApiService.getAnimeDetails(animeId = animeId, fields = fields)
                AttemptResult.Success(dto)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                when {
                    e.code() == HTTP_NOT_FOUND -> {
                        AppLog.domain.w(e) { "$itemType $animeId not found on MAL" }
                        AttemptResult.NotFound
                    }
                    e.code() == HTTP_TOO_MANY_REQUESTS -> {
                        AttemptResult.Retryable(e, isRateLimit = true)
                    }
                    e.code() in HTTP_SERVER_ERROR_RANGE -> {
                        AttemptResult.Retryable(e, isRateLimit = false)
                    }
                    else -> throw e
                }
            } catch (e: IOException) {
                AttemptResult.Retryable(e, isRateLimit = false)
            }

        private suspend fun delayBeforeRetry(
            attempt: Int,
            isRateLimit: Boolean,
        ) {
            if (attempt < MAX_ITEM_ATTEMPTS) {
                val delayMs = if (isRateLimit) RATE_LIMIT_BACKOFF_MS else POLITE_DELAY_MS
                delay(delayMs.milliseconds)
            }
        }

        private class CandidateBatchState(
            val existingAnimeIds: Set<Long>,
        ) {
            val pendingAnimes = mutableListOf<AnimeEntity>()
            val pendingNewSeasons = mutableListOf<NewSeasonAnimeEntity>()
            val savedAnimeIds = mutableSetOf<Long>()
            val retryableFailedCandidateIds = mutableSetOf<Long>()

            fun addCached(
                candidateId: Long,
                newSeason: NewSeasonAnimeEntity,
            ) {
                pendingNewSeasons.add(newSeason)
                savedAnimeIds.add(candidateId)
            }

            fun addSuccess(
                candidateId: Long,
                newSeason: NewSeasonAnimeEntity,
                anime: AnimeEntity,
            ) {
                pendingAnimes.add(anime)
                pendingNewSeasons.add(newSeason)
                savedAnimeIds.add(candidateId)
            }

            fun addRetryableFailure(candidateId: Long) {
                retryableFailedCandidateIds.add(candidateId)
            }

            fun shouldFlush(): Boolean = pendingNewSeasons.size >= BATCH_FLUSH_SIZE

            fun hasPending(): Boolean = pendingNewSeasons.isNotEmpty() || pendingAnimes.isNotEmpty()

            fun clearPending() {
                pendingAnimes.clear()
                pendingNewSeasons.clear()
            }
        }

        private sealed interface FetchItemResult {
            data class Success(
                val details: AnimeDetailsDto,
            ) : FetchItemResult

            data object NotFound : FetchItemResult

            data class RetryableError(
                val exception: Exception,
            ) : FetchItemResult
        }

        private sealed interface AttemptResult {
            data class Success(
                val details: AnimeDetailsDto,
            ) : AttemptResult

            data object NotFound : AttemptResult

            data class Retryable(
                val exception: Exception,
                val isRateLimit: Boolean,
            ) : AttemptResult
        }

        private data class CandidateCollectionResult(
            val candidateMap: Map<Long, CandidateRelation>,
            val failedParentAnimeIds: Set<Long>,
        )

        private data class CandidateFetchResult(
            val savedAnimeIds: Set<Long>,
            val retryableFailedCandidateIds: Set<Long>,
            val unreachedCandidateIds: Set<Long> = emptySet(),
        )

        private data class CandidateRelation(
            val parentAnimeId: Long,
            val relationType: String,
            val relationTypeFormatted: String,
        )

        companion object {
            private const val MIN_USER_LIST_THRESHOLD = 5
            private const val POLITE_DELAY_MS = 300L
            private const val RATE_LIMIT_BACKOFF_MS = 1500L
            private const val MAX_ITEM_ATTEMPTS = 2
            private const val BATCH_FLUSH_SIZE = 5
            private const val MAX_UNCACHED_FETCHES_PER_RUN = 50
            private const val HTTP_NOT_FOUND = 404
            private const val HTTP_TOO_MANY_REQUESTS = 429
            private val HTTP_SERVER_ERROR_RANGE = 500..599

            private val NARRATIVE_RELATIONS =
                setOf(
                    "sequel",
                    "prequel",
                    "side_story",
                    "parent_story",
                    "spin_off",
                    "alternative_setting",
                    "alternative_version",
                    "full_story",
                )
        }
    }
