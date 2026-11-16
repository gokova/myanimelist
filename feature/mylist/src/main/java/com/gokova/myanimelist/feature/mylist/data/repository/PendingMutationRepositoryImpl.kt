@file:Suppress("TooManyFunctions")

package com.gokova.myanimelist.feature.mylist.data.repository

import com.gokova.myanimelist.core.database.dao.PendingUserAnimeMutationDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.PendingUserAnimeMutationEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.database.util.DatabaseTransactionRunner
import com.gokova.myanimelist.core.database.util.UserAnimeListSyncCoordinator
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.domain.model.ConflictResolution
import com.gokova.myanimelist.core.domain.model.PendingAnimeMutation
import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.repository.PendingMutationRepository
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.di.IoDispatcher
import com.gokova.myanimelist.core.network.model.MyListStatusDto
import com.gokova.myanimelist.feature.mylist.data.mapper.PendingMutationMapper
import com.gokova.myanimelist.feature.mylist.data.work.PendingMutationScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions", "LongParameterList")
class PendingMutationRepositoryImpl
    @Inject
    constructor(
        private val pendingMutationDao: PendingUserAnimeMutationDao,
        private val userAnimeListDao: UserAnimeListDao,
        private val malApiService: MalApiService,
        private val pendingMutationScheduler: PendingMutationScheduler,
        private val syncCoordinator: UserAnimeListSyncCoordinator,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val transactionRunner: DatabaseTransactionRunner =
            object : DatabaseTransactionRunner {
                override suspend fun <T> invoke(block: suspend () -> T): T = block()
            },
    ) : PendingMutationRepository {
        override fun observePendingMutations(): Flow<List<PendingAnimeMutation>> =
            pendingMutationDao
                .observeAllPendingMutations()
                .map { list -> list.map { PendingMutationMapper.toDomain(it) } }

        override fun observePendingMutation(animeId: Long): Flow<PendingAnimeMutation?> =
            pendingMutationDao
                .observePendingMutationById(animeId)
                .map { it?.let { PendingMutationMapper.toDomain(it) } }

        override suspend fun getPendingMutations(): List<PendingAnimeMutation> =
            withContext(ioDispatcher) {
                pendingMutationDao.getAllPendingMutations().map {
                    PendingMutationMapper.toDomain(it)
                }
            }

        override suspend fun getPendingMutation(animeId: Long): PendingAnimeMutation? =
            withContext(ioDispatcher) {
                pendingMutationDao.getPendingMutationById(animeId)?.let {
                    PendingMutationMapper.toDomain(it)
                }
            }

        override suspend fun saveMutationLocally(
            animeId: Long,
            params: UpdateUserAnimeStatusParams,
        ): Result<Unit> =
            syncCoordinator.withLock {
                withContext(ioDispatcher) {
                    saveMutationLocallyInternal(animeId, params)
                }
            }

        private suspend fun saveMutationLocallyInternal(
            animeId: Long,
            params: UpdateUserAnimeStatusParams,
        ): Result<Unit> {
            return try {
                val currentItem =
                    userAnimeListDao.getUserAnimeById(animeId)
                        ?: return Result.failure(
                            IllegalStateException("Anime $animeId not found in local user list"),
                        )
                val existing = pendingMutationDao.getPendingMutationById(animeId)
                val (updatedUserAnime, mutation) =
                    buildCoalescedState(animeId, currentItem.userAnime, existing, params)

                transactionRunner {
                    userAnimeListDao.upsertUserAnimeList(listOf(updatedUserAnime))
                    pendingMutationDao.upsertPendingMutation(mutation)
                }
                Result.success(Unit)
            } catch (ce: CancellationException) {
                throw ce
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                AppLog.data.e(e) { "Failed to save mutation locally for animeId=$animeId" }
                Result.failure(e)
            }
        }

        private fun buildCoalescedState(
            animeId: Long,
            current: UserAnimeListEntity,
            existing: PendingUserAnimeMutationEntity?,
            params: UpdateUserAnimeStatusParams,
        ): Pair<UserAnimeListEntity, PendingUserAnimeMutationEntity> {
            val baselineStatus = existing?.baselineStatus ?: current.status
            val baselineEp = existing?.baselineEpisodes ?: current.numEpisodesWatched
            val baselineScore = existing?.baselineScore ?: current.score
            val baselineRewatch = existing?.baselineIsRewatching ?: current.isRewatching

            val desiredStatus = params.status ?: existing?.status ?: current.status
            val desiredEp =
                params.numWatchedEpisodes ?: existing?.numEpisodesWatched
                    ?: current.numEpisodesWatched
            val desiredScore = params.score ?: existing?.score ?: current.score
            val desiredRewatch =
                params.isRewatching ?: existing?.isRewatching ?: current.isRewatching

            val updatedUserAnime =
                current.copy(
                    status = desiredStatus,
                    score = desiredScore,
                    numEpisodesWatched = desiredEp,
                    isRewatching = desiredRewatch,
                )
            val mutation =
                PendingUserAnimeMutationEntity(
                    animeId = animeId,
                    status = desiredStatus,
                    numEpisodesWatched = desiredEp,
                    score = desiredScore,
                    isRewatching = desiredRewatch,
                    baselineStatus = baselineStatus,
                    baselineEpisodes = baselineEp,
                    baselineScore = baselineScore,
                    baselineIsRewatching = baselineRewatch,
                    syncState = PendingUserAnimeMutationEntity.SYNC_STATE_PENDING,
                )
            return updatedUserAnime to mutation
        }

        override suspend fun resolveConflict(
            animeId: Long,
            resolution: ConflictResolution,
        ): Result<Unit> =
            withContext(ioDispatcher) {
                try {
                    when (resolution) {
                        is ConflictResolution.KeepDevice ->
                            applyKeepDeviceResolution(animeId)
                        is ConflictResolution.UseRemote ->
                            applyUseRemoteResolution(animeId)
                        is ConflictResolution.CustomEdit -> {
                            val saveResult =
                                saveMutationLocally(animeId, resolution.params)
                            if (saveResult.isSuccess) {
                                pendingMutationScheduler.scheduleSync()
                            }
                            saveResult
                        }
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.data.e(e) { "Failed to resolve conflict for animeId=$animeId" }
                    Result.failure(e)
                }
            }

        private suspend fun applyKeepDeviceResolution(animeId: Long): Result<Unit> {
            val mutation =
                pendingMutationDao.getPendingMutationById(animeId)
                    ?: return Result.failure(
                        IllegalStateException("No pending mutation for $animeId"),
                    )
            val updatedDto =
                malApiService.updateMyListStatus(
                    animeId = animeId,
                    status = mutation.status,
                    isRewatching = mutation.isRewatching,
                    score = mutation.score,
                    numWatchedEpisodes = mutation.numEpisodesWatched,
                )
            commitConfirmedRemoteStatus(animeId, updatedDto, mutation)
            return Result.success(Unit)
        }

        private suspend fun applyUseRemoteResolution(animeId: Long): Result<Unit> =
            syncCoordinator.withLock {
                val mutation =
                    pendingMutationDao.getPendingMutationById(animeId)
                        ?: return@withLock Result.failure(
                            IllegalStateException("No pending mutation for $animeId"),
                        )
                if (mutation.syncState != PendingUserAnimeMutationEntity.SYNC_STATE_CONFLICT) {
                    return@withLock Result.success(Unit)
                }
                val currentItem = userAnimeListDao.getUserAnimeById(animeId)
                if (currentItem != null) {
                    val updated =
                        currentItem.userAnime.copy(
                            status = mutation.remoteStatus ?: currentItem.userAnime.status,
                            score = mutation.remoteScore ?: currentItem.userAnime.score,
                            numEpisodesWatched =
                                mutation.remoteEpisodes ?: currentItem.userAnime.numEpisodesWatched,
                            isRewatching =
                                mutation.remoteIsRewatching ?: currentItem.userAnime.isRewatching,
                        )
                    transactionRunner {
                        userAnimeListDao.upsertUserAnimeList(listOf(updated))
                        pendingMutationDao.deletePendingMutation(animeId)
                    }
                } else {
                    pendingMutationDao.deletePendingMutation(animeId)
                }
                Result.success(Unit)
            }

        override suspend fun discardMutation(animeId: Long): Result<Unit> =
            syncCoordinator.withLock {
                withContext(ioDispatcher) {
                    try {
                        val mutation =
                            pendingMutationDao.getPendingMutationById(animeId)
                                ?: return@withContext Result.failure(
                                    IllegalStateException("No pending mutation for $animeId"),
                                )
                        val currentItem = userAnimeListDao.getUserAnimeById(animeId)
                        if (currentItem != null) {
                            val restored =
                                currentItem.userAnime.copy(
                                    status =
                                        mutation.baselineStatus ?: currentItem.userAnime.status,
                                    score =
                                        mutation.baselineScore ?: currentItem.userAnime.score,
                                    numEpisodesWatched =
                                        mutation.baselineEpisodes
                                            ?: currentItem.userAnime.numEpisodesWatched,
                                    isRewatching =
                                        mutation.baselineIsRewatching
                                            ?: currentItem.userAnime.isRewatching,
                                )
                            transactionRunner {
                                userAnimeListDao.upsertUserAnimeList(listOf(restored))
                                pendingMutationDao.deletePendingMutation(animeId)
                            }
                        } else {
                            pendingMutationDao.deletePendingMutation(animeId)
                        }
                        Result.success(Unit)
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        AppLog.data.e(e) { "Failed to discard mutation for animeId=$animeId" }
                        Result.failure(e)
                    }
                }
            }

        override suspend fun clearAllPendingMutations(): Result<Unit> =
            withContext(ioDispatcher) {
                pendingMutationDao.clearAllPendingMutations()
                Result.success(Unit)
            }

        override suspend fun getPendingCount(): Int =
            withContext(ioDispatcher) {
                pendingMutationDao.getPendingCount()
            }

        override suspend fun syncMutation(animeId: Long): Result<Unit> =
            withContext(ioDispatcher) {
                val mutation =
                    pendingMutationDao.getPendingMutationById(animeId)
                        ?: return@withContext Result.success(Unit)

                val isBlockedState =
                    mutation.syncState == PendingUserAnimeMutationEntity.SYNC_STATE_CONFLICT ||
                        mutation.syncState ==
                        PendingUserAnimeMutationEntity.SYNC_STATE_AUTH_REQUIRED
                if (isBlockedState) {
                    return@withContext Result.success(Unit)
                }

                executeSyncForMutation(mutation)
            }

        override suspend fun syncAllPending(): Result<Unit> =
            withContext(ioDispatcher) {
                val all = pendingMutationDao.getAllPendingMutations()
                var retryableFailure: Throwable? = null
                var nonRetryableFailure: Throwable? = null

                for (mutation in all) {
                    val isBlockedState =
                        mutation.syncState ==
                            PendingUserAnimeMutationEntity.SYNC_STATE_CONFLICT ||
                            mutation.syncState ==
                            PendingUserAnimeMutationEntity.SYNC_STATE_AUTH_REQUIRED
                    if (isBlockedState) {
                        continue
                    }

                    val result = executeSyncForMutation(mutation)
                    if (result.isFailure) {
                        val ex = result.exceptionOrNull()
                        if (isRetryableError(ex)) {
                            if (retryableFailure == null) {
                                retryableFailure = ex
                            }
                        } else if (nonRetryableFailure == null) {
                            nonRetryableFailure = ex
                        }
                    }
                }

                val failureToReport = retryableFailure ?: nonRetryableFailure
                if (failureToReport != null) {
                    Result.failure(failureToReport)
                } else {
                    Result.success(Unit)
                }
            }

        private suspend fun executeSyncForMutation(
            mutation: PendingUserAnimeMutationEntity,
        ): Result<Unit> =
            try {
                pendingMutationDao.updateSyncState(
                    mutation.animeId,
                    PendingUserAnimeMutationEntity.SYNC_STATE_SYNCING,
                )

                val details =
                    malApiService.getAnimeDetails(
                        animeId = mutation.animeId,
                        fields = MalApiService.DEFAULT_ANIME_DETAILS_FIELDS,
                    )
                val remoteStatus = details.myListStatus

                if (matchesDesired(mutation, remoteStatus)) {
                    AppLog.data.i { "Mutation for ${mutation.animeId} already matches MAL remote" }
                    pendingMutationDao.deletePendingMutation(mutation.animeId)
                    Result.success(Unit)
                } else if (isConflict(mutation, remoteStatus)) {
                    recordConflict(mutation, remoteStatus)
                } else {
                    performAutoMerge(mutation)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                handleSyncException(mutation.animeId, e)
            }

        private suspend fun recordConflict(
            mutation: PendingUserAnimeMutationEntity,
            remoteStatus: MyListStatusDto?,
        ): Result<Unit> {
            AppLog.data.w { "Conflict detected for animeId=${mutation.animeId}" }
            pendingMutationDao.updateConflict(
                animeId = mutation.animeId,
                remoteStatus = remoteStatus?.status,
                remoteEpisodes = remoteStatus?.numEpisodesWatched,
                remoteScore = remoteStatus?.score,
                remoteIsRewatching = remoteStatus?.isRewatching,
                syncState = PendingUserAnimeMutationEntity.SYNC_STATE_CONFLICT,
            )
            return Result.failure(
                IllegalStateException("Conflict detected for animeId=${mutation.animeId}"),
            )
        }

        private suspend fun performAutoMerge(
            mutation: PendingUserAnimeMutationEntity,
        ): Result<Unit> {
            val sendStatus =
                if (mutation.status != mutation.baselineStatus) mutation.status else null
            val sendRewatch =
                if (mutation.isRewatching != mutation.baselineIsRewatching) {
                    mutation.isRewatching
                } else {
                    null
                }
            val sendScore =
                if (mutation.score != mutation.baselineScore) mutation.score else null
            val sendEp =
                if (mutation.numEpisodesWatched != mutation.baselineEpisodes) {
                    mutation.numEpisodesWatched
                } else {
                    null
                }

            val hasModifications =
                sendStatus != null || sendRewatch != null || sendScore != null || sendEp != null
            if (!hasModifications) {
                AppLog.data.i {
                    "No local modifications relative to baseline for ${mutation.animeId}"
                }
                pendingMutationDao.deletePendingMutation(mutation.animeId)
                return Result.success(Unit)
            }

            val updatedDto =
                malApiService.updateMyListStatus(
                    animeId = mutation.animeId,
                    status = sendStatus,
                    isRewatching = sendRewatch,
                    score = sendScore,
                    numWatchedEpisodes = sendEp,
                )
            commitConfirmedRemoteStatus(mutation.animeId, updatedDto, mutation)
            AppLog.data.i { "Successfully synced mutation for animeId=${mutation.animeId}" }
            return Result.success(Unit)
        }

        private suspend fun handleSyncException(
            animeId: Long,
            exception: Exception,
        ): Result<Unit> {
            if (exception is HttpException) {
                if (exception.code() == HTTP_UNAUTHORIZED) {
                    AppLog.data.w { "Auth expired during sync for $animeId" }
                    pendingMutationDao.updateSyncState(
                        animeId,
                        PendingUserAnimeMutationEntity.SYNC_STATE_AUTH_REQUIRED,
                    )
                } else {
                    AppLog.data.w(exception) { "HTTP error ${exception.code()} for $animeId" }
                    pendingMutationDao.updateSyncState(
                        animeId,
                        PendingUserAnimeMutationEntity.SYNC_STATE_FAILED,
                        exception.message,
                    )
                }
            } else if (exception is IOException) {
                AppLog.data.d {
                    "Network unavailable during sync for $animeId; keeping in PENDING"
                }
                pendingMutationDao.updateSyncState(
                    animeId,
                    PendingUserAnimeMutationEntity.SYNC_STATE_PENDING,
                )
            } else {
                AppLog.data.w(exception) { "Error syncing mutation for animeId=$animeId" }
                pendingMutationDao.updateSyncState(
                    animeId,
                    PendingUserAnimeMutationEntity.SYNC_STATE_FAILED,
                    exception.message,
                )
            }
            return Result.failure(exception)
        }

        private suspend fun commitConfirmedRemoteStatus(
            animeId: Long,
            updatedDto: MyListStatusDto,
            syncedMutation: PendingUserAnimeMutationEntity? = null,
        ) = syncCoordinator.withLock {
            val currentItem = userAnimeListDao.getUserAnimeById(animeId)
            val currentPending = pendingMutationDao.getPendingMutationById(animeId)
            val wasModified = isModifiedDuringSync(currentPending, syncedMutation)

            if (currentItem != null) {
                val confirmed = buildConfirmedUserAnime(currentItem, updatedDto, wasModified)
                transactionRunner {
                    userAnimeListDao.upsertUserAnimeList(listOf(confirmed))
                    updateOrDeletePendingAfterSync(
                        animeId,
                        currentPending,
                        updatedDto,
                        wasModified,
                    )
                }
            } else if (!wasModified) {
                pendingMutationDao.deletePendingMutation(animeId)
            }
        }

        private fun buildConfirmedUserAnime(
            currentItem: UserAnimeListItem,
            updatedDto: MyListStatusDto,
            wasModified: Boolean,
        ): UserAnimeListEntity {
            val userAnime = currentItem.userAnime
            if (wasModified) {
                return userAnime.copy(
                    updatedAt = updatedDto.updatedAt ?: userAnime.updatedAt,
                )
            }
            return userAnime.copy(
                status = updatedDto.status ?: userAnime.status,
                score = updatedDto.score ?: userAnime.score,
                numEpisodesWatched = updatedDto.numEpisodesWatched ?: userAnime.numEpisodesWatched,
                updatedAt = updatedDto.updatedAt ?: userAnime.updatedAt,
                isRewatching = updatedDto.isRewatching ?: userAnime.isRewatching,
            )
        }

        private suspend fun updateOrDeletePendingAfterSync(
            animeId: Long,
            currentPending: PendingUserAnimeMutationEntity?,
            updatedDto: MyListStatusDto,
            wasModified: Boolean,
        ) {
            if (!wasModified) {
                pendingMutationDao.deletePendingMutation(animeId)
            } else if (currentPending != null) {
                pendingMutationDao.upsertPendingMutation(
                    currentPending.copy(
                        baselineStatus = updatedDto.status ?: currentPending.baselineStatus,
                        baselineEpisodes =
                            updatedDto.numEpisodesWatched ?: currentPending.baselineEpisodes,
                        baselineScore = updatedDto.score ?: currentPending.baselineScore,
                        baselineIsRewatching =
                            updatedDto.isRewatching ?: currentPending.baselineIsRewatching,
                        syncState = PendingUserAnimeMutationEntity.SYNC_STATE_PENDING,
                    ),
                )
            }
        }

        private fun isModifiedDuringSync(
            current: PendingUserAnimeMutationEntity?,
            synced: PendingUserAnimeMutationEntity?,
        ): Boolean {
            if (current == null || synced == null) return false
            val epChanged = current.numEpisodesWatched != synced.numEpisodesWatched
            val statusChanged = current.status != synced.status
            val scoreChanged = current.score != synced.score
            val rewatchChanged = current.isRewatching != synced.isRewatching
            return epChanged || statusChanged || scoreChanged || rewatchChanged
        }

        private fun isRetryableError(throwable: Throwable?): Boolean =
            when (throwable) {
                is IOException -> true
                is HttpException -> {
                    throwable.code() in SERVER_ERROR_RANGE ||
                        throwable.code() == HTTP_TOO_MANY_REQUESTS
                }
                else -> false
            }

        override suspend fun resetAuthRequiredMutations(): Result<Unit> =
            withContext(ioDispatcher) {
                try {
                    val count = pendingMutationDao.resetAuthRequiredMutations()
                    if (count > 0) {
                        AppLog.data.i { "Reset $count AUTH_REQUIRED mutations to PENDING" }
                        pendingMutationScheduler.scheduleSync()
                    }
                    Result.success(Unit)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.data.e(e) { "Failed to reset AUTH_REQUIRED mutations" }
                    Result.failure(e)
                }
            }

        private fun matchesDesired(
            mutation: PendingUserAnimeMutationEntity,
            remote: MyListStatusDto?,
        ): Boolean {
            if (remote == null) return false
            val epMatch =
                mutation.numEpisodesWatched == null ||
                    mutation.numEpisodesWatched == (remote.numEpisodesWatched ?: 0)
            val statusMatch = mutation.status == null || mutation.status == remote.status
            val scoreMatch =
                mutation.score == null ||
                    mutation.score == (remote.score ?: 0)
            val rewatchMatch =
                mutation.isRewatching == null ||
                    mutation.isRewatching == (remote.isRewatching ?: false)

            val progressAndStatusMatch = epMatch && statusMatch
            val scoreAndRewatchMatch = scoreMatch && rewatchMatch
            return progressAndStatusMatch && scoreAndRewatchMatch
        }

        private fun isConflict(
            mutation: PendingUserAnimeMutationEntity,
            remote: MyListStatusDto?,
        ): Boolean {
            if (remote == null) return false
            val progressOrStatusConflict =
                isFieldConflicted(
                    mutation.numEpisodesWatched,
                    mutation.baselineEpisodes,
                    remote.numEpisodesWatched,
                ) ||
                    isFieldConflicted(mutation.status, mutation.baselineStatus, remote.status)

            val scoreOrRewatchConflict =
                isFieldConflicted(
                    mutation.score,
                    mutation.baselineScore,
                    remote.score,
                ) ||
                    isFieldConflicted(
                        mutation.isRewatching,
                        mutation.baselineIsRewatching,
                        remote.isRewatching,
                    )

            return progressOrStatusConflict || scoreOrRewatchConflict
        }

        private fun isFieldConflicted(
            desired: Any?,
            baseline: Any?,
            remote: Any?,
        ): Boolean {
            if (desired == null || desired == baseline || remote == null) return false
            return remote != baseline && remote != desired
        }

        companion object {
            private const val HTTP_UNAUTHORIZED = 401
            private const val HTTP_TOO_MANY_REQUESTS = 429
            private val SERVER_ERROR_RANGE = 500..599
        }
    }
