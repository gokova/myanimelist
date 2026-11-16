package com.gokova.myanimelist.feature.mylist.data.repository

import com.gokova.myanimelist.core.database.dao.PendingUserAnimeMutationDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.PendingUserAnimeMutationEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.util.UserAnimeListSyncCoordinator
import com.gokova.myanimelist.core.datastore.SyncPreferences
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.feature.mylist.data.mapper.AnimeListMapper
import com.gokova.myanimelist.feature.mylist.data.mapper.PendingMutationMapper
import com.gokova.myanimelist.feature.mylist.data.remote.AnimeListRemoteDataSource
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SyncStatus
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.repository.MyListRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyListRepositoryImpl
    @Inject
    constructor(
        private val dao: UserAnimeListDao,
        private val pendingMutationDao: PendingUserAnimeMutationDao,
        private val remoteDataSource: AnimeListRemoteDataSource,
        private val syncPreferences: SyncPreferences,
        private val syncCoordinator: UserAnimeListSyncCoordinator = UserAnimeListSyncCoordinator(),
    ) : MyListRepository {
        override fun observeUserAnimeList(category: ListFilterCategory): Flow<List<UserAnime>> {
            AppLog.data.d { "observeUserAnimeList for category=${category.name}" }
            val baseFlow =
                if (category.status != null) {
                    dao.observeUserAnimeByStatus(category.status.apiValue)
                } else {
                    dao.observeAllUserAnime()
                }

            return combine(
                baseFlow,
                pendingMutationDao.observeAllPendingMutations(),
            ) { items, pendingEntities ->
                val pendingMap = pendingEntities.associateBy { it.animeId }
                items.map { item ->
                    val pendingEntity = pendingMap[item.userAnime.animeId]
                    val pendingDomain = pendingEntity?.let { PendingMutationMapper.toDomain(it) }
                    AnimeListMapper.toDomain(item, pendingDomain)
                }
            }
        }

        @Suppress("TooGenericExceptionCaught")
        override fun syncUserAnimeList(force: Boolean): Flow<SyncStatus> =
            flow {
                try {
                    val currentTime = System.currentTimeMillis()
                    if (!force) {
                        val lastSyncTime = syncPreferences.getLastAnimeListSyncTimestamp()
                        if (currentTime - lastSyncTime < SYNC_COOLDOWN_MS) {
                            AppLog.data.d { "Sync skipped: cooldown still active" }
                            emit(SyncStatus.SkippedCooldown)
                            return@flow
                        }
                    }

                    emit(SyncStatus.Started)

                    val allEntries = remoteDataSource.fetchAllUserAnime()
                    AppLog.data.i { "Fetched ${allEntries.size} anime entries from MAL API" }
                    val animes = allEntries.map { AnimeListMapper.toAnimeEntity(it) }
                    val userAnimeList =
                        allEntries.map { AnimeListMapper.toUserAnimeListEntity(it) }

                    syncCoordinator.withLock {
                        dao.syncUserAnimeList(animes, userAnimeList)
                        reconcilePendingMutationsAfterSync(userAnimeList)

                        AppLog.data.d {
                            "Persisted ${animes.size} animes & ${userAnimeList.size} records to Room"
                        }
                        syncPreferences.updateLastAnimeListSyncTimestamp(currentTime)
                    }

                    emit(SyncStatus.Completed)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLog.data.e(e) { "Exception occurred during syncUserAnimeList" }
                    emit(SyncStatus.Failure(e))
                }
            }

        private suspend fun reconcilePendingMutationsAfterSync(
            incomingList: List<UserAnimeListEntity>,
        ) {
            val incomingMap = incomingList.associateBy { it.animeId }
            val pendingList = pendingMutationDao.getAllPendingMutations()
            for (pending in pendingList) {
                val remote = incomingMap[pending.animeId] ?: continue
                val epMatch =
                    pending.numEpisodesWatched == null ||
                        pending.numEpisodesWatched == remote.numEpisodesWatched
                val statusMatch = pending.status == null || pending.status == remote.status
                val scoreMatch = pending.score == null || pending.score == remote.score
                val rewatchMatch =
                    pending.isRewatching == null || pending.isRewatching == remote.isRewatching

                val progressAndStatusMatch = epMatch && statusMatch
                val scoreAndRewatchMatch = scoreMatch && rewatchMatch
                if (progressAndStatusMatch && scoreAndRewatchMatch) {
                    pendingMutationDao.deletePendingMutation(pending.animeId)
                } else if (hasConflictWithRemote(pending, remote)) {
                    pendingMutationDao.updateConflict(
                        animeId = pending.animeId,
                        remoteStatus = remote.status,
                        remoteEpisodes = remote.numEpisodesWatched,
                        remoteScore = remote.score,
                        remoteIsRewatching = remote.isRewatching,
                        syncState = PendingUserAnimeMutationEntity.SYNC_STATE_CONFLICT,
                    )
                }
            }
        }

        private fun hasConflictWithRemote(
            pending: PendingUserAnimeMutationEntity,
            remote: UserAnimeListEntity,
        ): Boolean {
            val progressOrStatusConflict =
                isFieldConflicted(
                    pending.numEpisodesWatched,
                    pending.baselineEpisodes,
                    remote.numEpisodesWatched,
                ) ||
                    isFieldConflicted(pending.status, pending.baselineStatus, remote.status)
            val scoreOrRewatchConflict =
                isFieldConflicted(
                    pending.score,
                    pending.baselineScore,
                    remote.score,
                ) ||
                    isFieldConflicted(
                        pending.isRewatching,
                        pending.baselineIsRewatching,
                        remote.isRewatching,
                    )
            return progressOrStatusConflict || scoreOrRewatchConflict
        }

        private fun isFieldConflicted(
            desired: Any?,
            baseline: Any?,
            remote: Any?,
        ): Boolean {
            if (desired == null || desired == baseline) return false
            return remote != baseline && remote != desired
        }

        companion object {
            private val SYNC_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(15)
        }
    }
