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
import com.gokova.myanimelist.core.network.model.RelatedAnimeEdgeDto
import com.gokova.myanimelist.feature.recommendation.data.mapper.RecommendationMapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        private val syncTracker: NewSeasonSyncTracker,
    ) : CoroutineWorker(context, params) {
        private val workScope: String
            get() =
                if (inputData.getBoolean(NewSeasonScheduler.KEY_IS_MANUAL, false)) {
                    NewSeasonSyncTracker.SCOPE_MANUAL
                } else {
                    NewSeasonSyncTracker.SCOPE_PERIODIC
                }

        override suspend fun doWork(): Result =
            syncMutex.withLock {
                AppLog.domain.i { "FetchNewSeasonsWorker started (attempt $runAttemptCount)" }
                try {
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
                        syncTracker.reset(workScope)
                        Result.success()
                    } else {
                        val hasMoreWorkOrErrors = processNewSeasons(userAnimeList)
                        if (hasMoreWorkOrErrors) {
                            AppLog.domain.i {
                                "FetchNewSeasonsWorker batch complete, scheduling next batch"
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
            if (runAttemptCount == 0) {
                syncTracker.reset(workScope)
                syncTracker.setSyncStartTime(System.currentTimeMillis(), workScope)
            }

            val syncStartTime =
                syncTracker.getSyncStartTime(workScope).let { start ->
                    if (start <= 0L) {
                        val now = System.currentTimeMillis()
                        syncTracker.setSyncStartTime(now, workScope)
                        now
                    } else {
                        start
                    }
                }
            val lastProcessedId = syncTracker.getLastProcessedUserAnimeId(workScope)

            val sortedUserList = userAnimeList.sortedBy { it.anime.id }
            val remainingUserAnime = sortedUserList.filter { it.anime.id > lastProcessedId }
            val userAnimeIds = userAnimeList.map { it.anime.id }.toSet()

            if (remainingUserAnime.isEmpty()) {
                AppLog.domain.i { "All user anime already processed, finalizing new seasons" }
                pruneObsoleteSeasons(syncStartTime, userAnimeIds, emptySet())
                syncTracker.reset(workScope)
                return false
            }

            val currentBatch = remainingUserAnime.take(USER_ANIME_BATCH_SIZE)

            val batchResult = processBatch(currentBatch, userAnimeIds)

            val isFinished =
                remainingUserAnime.size <= currentBatch.size && !batchResult.hasRetryableErrors
            if (isFinished) {
                pruneObsoleteSeasons(syncStartTime, userAnimeIds, batchResult.failedParentAnimeIds)
                syncTracker.reset(workScope)
                AppLog.domain.i { "FetchNewSeasonsWorker completed all batches successfully" }
            } else {
                AppLog.domain.i {
                    "FetchNewSeasonsWorker batch complete. Saved: ${batchResult.savedCount}, " +
                        "remaining user anime: ${remainingUserAnime.size - currentBatch.size}"
                }
            }

            return !isFinished
        }

        private suspend fun processBatch(
            currentBatch: List<UserAnimeListItem>,
            userAnimeIds: Set<Long>,
        ): BatchResult {
            val visitedAnimeIds = userAnimeIds.toMutableSet()
            val state = CandidateBatchState()
            val failedParentAnimeIds = mutableSetOf<Long>()
            var stoppedOrFailed = false

            for (userItem in currentBatch) {
                if (stoppedOrFailed || isStopped) {
                    break
                }
                val parentId = userItem.anime.id
                if (processUserAnimeTree(parentId, visitedAnimeIds, state)) {
                    syncTracker.setLastProcessedUserAnimeId(parentId, workScope)
                    delay(POLITE_DELAY_MS.milliseconds)
                } else {
                    failedParentAnimeIds.add(parentId)
                    stoppedOrFailed = true
                }
            }

            state.flushRemaining(newSeasonDao)

            return BatchResult(
                savedCount = state.savedAnimeIds.size,
                hasRetryableErrors = failedParentAnimeIds.isNotEmpty(),
                failedParentAnimeIds = failedParentAnimeIds,
            )
        }

        private suspend fun processUserAnimeTree(
            parentId: Long,
            visitedAnimeIds: MutableSet<Long>,
            state: CandidateBatchState,
        ): Boolean {
            val queue = ArrayDeque<CandidateQueueItem>()
            val collector = RecursiveCandidateCollector(visitedAnimeIds, queue)

            val fetchResult =
                fetchAnimeDetailsWithRetry(
                    animeId = parentId,
                    fields = MalApiService.FIELDS_RELATED_ANIME,
                    itemType = "User anime",
                )
            var hasTreeError =
                when (fetchResult) {
                    is FetchItemResult.Success -> {
                        collector.enqueueDirectEdges(fetchResult.details.relatedAnime, parentId)
                        false
                    }
                    is FetchItemResult.NotFound -> {
                        AppLog.domain.w { "User anime $parentId returned 404 Not Found" }
                        false
                    }
                    is FetchItemResult.RetryableError -> true
                }

            if (!hasTreeError && queue.isNotEmpty()) {
                val initialIds = queue.map { it.candidateId }
                val existingAnimeIds = newSeasonDao.getExistingAnimeIds(initialIds).toMutableSet()
                var processedCandidatesCount = 0

                while (queue.isNotEmpty() && !isStopped && !hasTreeError) {
                    if (processedCandidatesCount >= MAX_CANDIDATES_PER_ROOT) {
                        AppLog.domain.w {
                            "Reached max candidates limit ($MAX_CANDIDATES_PER_ROOT) for parent $parentId"
                        }
                        break
                    }
                    val item = queue.removeFirst()
                    val success = processQueueItem(item, existingAnimeIds, state, collector)
                    if (!success) {
                        hasTreeError = true
                    }
                    processedCandidatesCount++
                    state.flushIfFull(newSeasonDao)
                }
                if (isStopped) {
                    AppLog.domain.i { "FetchNewSeasonsWorker stopped in traversal" }
                    hasTreeError = true
                }
            }

            return !hasTreeError
        }

        private suspend fun processQueueItem(
            item: CandidateQueueItem,
            existingAnimeIds: MutableSet<Long>,
            state: CandidateBatchState,
            collector: RecursiveCandidateCollector,
        ): Boolean {
            val candidateId = item.candidateId
            val isContinuation = item.relation.relationType in CONTINUATION_RELATIONS
            val newSeason = item.toNewSeasonEntity()

            var success = true
            val isAlreadyCached =
                candidateId in existingAnimeIds ||
                    newSeasonDao.getExistingAnimeIds(listOf(candidateId)).isNotEmpty()
            if (isAlreadyCached) {
                existingAnimeIds.add(candidateId)
                state.addCached(candidateId, newSeason)
                if (isContinuation && item.depth < MAX_RECURSION_DEPTH) {
                    val relResult =
                        fetchAnimeDetailsWithRetry(
                            animeId = candidateId,
                            fields = MalApiService.FIELDS_RELATED_ANIME,
                            itemType = "Continuation",
                        )
                    when (relResult) {
                        is FetchItemResult.Success -> {
                            collector.discover(item, relResult.details.relatedAnime.orEmpty())
                        }
                        is FetchItemResult.NotFound -> Unit
                        is FetchItemResult.RetryableError -> success = false
                    }
                    delay(POLITE_DELAY_MS.milliseconds)
                }
            } else {
                val detailsResult =
                    fetchAnimeDetailsWithRetry(
                        animeId = candidateId,
                        fields = MalApiService.DEFAULT_ANIME_DETAILS_FIELDS,
                        itemType = "Candidate",
                    )
                when (detailsResult) {
                    is FetchItemResult.Success -> {
                        state.handleFetchSuccess(candidateId, newSeason, detailsResult.details)
                        existingAnimeIds.add(candidateId)
                        if (isContinuation && item.depth < MAX_RECURSION_DEPTH) {
                            collector.discover(item, detailsResult.details.relatedAnime.orEmpty())
                        }
                    }
                    is FetchItemResult.NotFound -> {
                        AppLog.domain.w { "Candidate $candidateId returned 404 Not Found" }
                    }
                    is FetchItemResult.RetryableError -> success = false
                }
                delay(POLITE_DELAY_MS.milliseconds)
            }
            return success
        }

        private suspend fun pruneObsoleteSeasons(
            syncStartTime: Long,
            userAnimeIds: Set<Long>,
            failedParentAnimeIds: Set<Long>,
        ) {
            val protectedChildIds =
                if (failedParentAnimeIds.isNotEmpty()) {
                    newSeasonDao.getNewSeasonAnimeIds(failedParentAnimeIds.toList()).toSet()
                } else {
                    emptySet()
                }

            val cutoffTime = (syncStartTime - PRUNE_OBSOLETE_WINDOW_MS).coerceAtLeast(0L)
            val timeObsoleteIds =
                newSeasonDao
                    .getObsoleteNewSeasonAnimeIds(cutoffTime)
                    .filter { it !in protectedChildIds }

            val inUserListIds =
                newSeasonDao
                    .getNewSeasonAnimeIds()
                    .filter { it in userAnimeIds }

            val obsoleteIds = (timeObsoleteIds + inUserListIds).distinct()

            if (obsoleteIds.isNotEmpty()) {
                newSeasonDao.saveNewSeasonsTransaction(discardedAnimeIds = obsoleteIds)
                AppLog.domain.i { "Pruned ${obsoleteIds.size} obsolete new season records" }
            }
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

        private class CandidateBatchState {
            val pendingAnimes = mutableListOf<AnimeEntity>()
            val pendingNewSeasons = mutableListOf<NewSeasonAnimeEntity>()
            val savedAnimeIds = mutableSetOf<Long>()

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

            fun handleFetchSuccess(
                candidateId: Long,
                newSeason: NewSeasonAnimeEntity,
                details: AnimeDetailsDto,
            ) {
                val anime = RecommendationMapper.toAnimeEntity(details.toAnimeNodeDto())
                addSuccess(candidateId, newSeason, anime)
            }

            suspend fun flushIfFull(newSeasonDao: NewSeasonDao) {
                if (pendingNewSeasons.size >= BATCH_FLUSH_SIZE) {
                    newSeasonDao.saveNewSeasonsTransaction(
                        animes = pendingAnimes.toList(),
                        newSeasons = pendingNewSeasons.toList(),
                    )
                    clearPending()
                }
            }

            suspend fun flushRemaining(newSeasonDao: NewSeasonDao) {
                if (pendingNewSeasons.isNotEmpty() || pendingAnimes.isNotEmpty()) {
                    newSeasonDao.saveNewSeasonsTransaction(
                        animes = pendingAnimes.toList(),
                        newSeasons = pendingNewSeasons.toList(),
                    )
                    clearPending()
                }
            }

            private fun clearPending() {
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

        private data class CandidateQueueItem(
            val candidateId: Long,
            val relation: CandidateRelation,
            val depth: Int,
        ) {
            fun toNewSeasonEntity(): NewSeasonAnimeEntity =
                NewSeasonAnimeEntity(
                    animeId = candidateId,
                    parentAnimeId = relation.parentAnimeId,
                    relationType = relation.relationType,
                    relationTypeFormatted = relation.relationTypeFormatted,
                    createdAt = System.currentTimeMillis(),
                )
        }

        private class RecursiveCandidateCollector(
            private val visitedAnimeIds: MutableSet<Long>,
            private val queue: ArrayDeque<CandidateQueueItem>,
        ) {
            fun enqueueDirectEdges(
                edges: List<RelatedAnimeEdgeDto>?,
                parentAnimeId: Long,
            ) {
                if (edges.isNullOrEmpty()) return
                for (edge in edges) {
                    val childId = edge.node.id
                    if (childId !in visitedAnimeIds && edge.relationType in NARRATIVE_RELATIONS) {
                        visitedAnimeIds.add(childId)
                        queue.addLast(
                            CandidateQueueItem(
                                candidateId = childId,
                                relation =
                                    CandidateRelation(
                                        parentAnimeId = parentAnimeId,
                                        relationType = edge.relationType,
                                        relationTypeFormatted = edge.relationTypeFormatted,
                                    ),
                                depth = 1,
                            ),
                        )
                    }
                }
            }

            fun discover(
                parentItem: CandidateQueueItem,
                relatedEdges: List<RelatedAnimeEdgeDto>,
            ) {
                if (parentItem.relation.relationType !in CONTINUATION_RELATIONS) return

                for (edge in relatedEdges) {
                    val childId = edge.node.id
                    val relType = edge.relationType

                    if (childId !in visitedAnimeIds && relType in NARRATIVE_RELATIONS) {
                        visitedAnimeIds.add(childId)
                        queue.addLast(
                            CandidateQueueItem(
                                candidateId = childId,
                                relation =
                                    CandidateRelation(
                                        parentAnimeId = parentItem.candidateId,
                                        relationType = edge.relationType,
                                        relationTypeFormatted = edge.relationTypeFormatted,
                                    ),
                                depth = parentItem.depth + 1,
                            ),
                        )
                    }
                }
            }
        }

        private data class BatchResult(
            val savedCount: Int,
            val hasRetryableErrors: Boolean,
            val failedParentAnimeIds: Set<Long>,
        )

        private data class CandidateRelation(
            val parentAnimeId: Long,
            val relationType: String,
            val relationTypeFormatted: String,
        )

        companion object {
            const val PRUNE_OBSOLETE_WINDOW_MS = 24 * 60 * 60 * 1000L
            private const val MIN_USER_LIST_THRESHOLD = 5
            private const val USER_ANIME_BATCH_SIZE = 10
            private const val MAX_CANDIDATES_PER_ROOT = 50
            private val syncMutex = Mutex()
            private const val POLITE_DELAY_MS = 300L
            private const val RATE_LIMIT_BACKOFF_MS = 1500L
            private const val MAX_ITEM_ATTEMPTS = 2
            private const val BATCH_FLUSH_SIZE = 5
            private const val MAX_RECURSION_DEPTH = 10
            private const val HTTP_NOT_FOUND = 404
            private const val HTTP_TOO_MANY_REQUESTS = 429
            private val HTTP_SERVER_ERROR_RANGE = 500..599

            private val CONTINUATION_RELATIONS =
                setOf(
                    "sequel",
                    "prequel",
                )

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
