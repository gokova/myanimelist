package com.gokova.myanimelist.feature.mylist.data.repository

import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.datastore.SyncPreferences
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.feature.mylist.data.mapper.AnimeListMapper
import com.gokova.myanimelist.feature.mylist.data.remote.AnimeListRemoteDataSource
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SyncStatus
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.repository.MyListRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyListRepositoryImpl
    @Inject
    constructor(
        private val dao: UserAnimeListDao,
        private val remoteDataSource: AnimeListRemoteDataSource,
        private val syncPreferences: SyncPreferences,
    ) : MyListRepository {
        override fun observeUserAnimeList(category: ListFilterCategory): Flow<List<UserAnime>> {
            AppLog.data.d { "observeUserAnimeList for category=${category.name}" }
            val baseFlow =
                if (category.status != null) {
                    dao.observeUserAnimeByStatus(category.status.apiValue)
                } else {
                    dao.observeAllUserAnime()
                }

            return baseFlow.map { items ->
                AppLog.data.d { "Loaded ${items.size} cached items for ${category.name}" }
                items.map { AnimeListMapper.toDomain(it) }
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

                    dao.syncUserAnimeList(animes, userAnimeList)
                    AppLog.data.d {
                        "Persisted ${animes.size} animes & ${userAnimeList.size} records to Room"
                    }
                    syncPreferences.updateLastAnimeListSyncTimestamp(currentTime)

                    emit(SyncStatus.Completed)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLog.data.e(e) { "Exception occurred during syncUserAnimeList" }
                    emit(SyncStatus.Failure(e))
                }
            }

        companion object {
            private val SYNC_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(15)
        }
    }
