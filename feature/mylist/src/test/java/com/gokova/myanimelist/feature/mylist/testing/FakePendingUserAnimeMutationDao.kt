package com.gokova.myanimelist.feature.mylist.testing

import com.gokova.myanimelist.core.database.dao.PendingUserAnimeMutationDao
import com.gokova.myanimelist.core.database.entity.PendingUserAnimeMutationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePendingUserAnimeMutationDao : PendingUserAnimeMutationDao {
    val pendingMap = MutableStateFlow<Map<Long, PendingUserAnimeMutationEntity>>(emptyMap())

    override fun observeAllPendingMutations(): Flow<List<PendingUserAnimeMutationEntity>> =
        pendingMap.map { it.values.toList() }

    override suspend fun getAllPendingMutations(): List<PendingUserAnimeMutationEntity> =
        pendingMap.value.values.toList()

    override suspend fun getPendingMutationById(animeId: Long): PendingUserAnimeMutationEntity? =
        pendingMap.value[animeId]

    override fun observePendingMutationById(animeId: Long): Flow<PendingUserAnimeMutationEntity?> =
        pendingMap.map { it[animeId] }

    override suspend fun upsertPendingMutation(mutation: PendingUserAnimeMutationEntity) {
        pendingMap.value += (mutation.animeId to mutation)
    }

    override suspend fun updateSyncState(
        animeId: Long,
        syncState: String,
        errorMessage: String?,
    ): Int {
        val existing = pendingMap.value[animeId] ?: return 0
        pendingMap.value += (
            animeId to
                existing.copy(syncState = syncState, syncErrorMessage = errorMessage)
        )
        return 1
    }

    override suspend fun updateConflict(
        animeId: Long,
        remoteStatus: String?,
        remoteEpisodes: Int?,
        remoteScore: Int?,
        remoteIsRewatching: Boolean?,
        syncState: String,
    ): Int {
        val existing = pendingMap.value[animeId] ?: return 0
        pendingMap.value += (
            animeId to
                existing.copy(
                    remoteStatus = remoteStatus,
                    remoteEpisodes = remoteEpisodes,
                    remoteScore = remoteScore,
                    remoteIsRewatching = remoteIsRewatching,
                    syncState = syncState,
                )
        )
        return 1
    }

    override suspend fun deletePendingMutation(animeId: Long): Int {
        val existed = pendingMap.value.containsKey(animeId)
        pendingMap.value -= animeId
        return if (existed) 1 else 0
    }

    override suspend fun clearAllPendingMutations(): Int {
        val size = pendingMap.value.size
        pendingMap.value = emptyMap()
        return size
    }

    override suspend fun getPendingCount(): Int = pendingMap.value.size

    override suspend fun resetAuthRequiredMutations(): Int {
        var count = 0
        pendingMap.value =
            pendingMap.value.mapValues { (_, entity) ->
                if (entity.syncState == "AUTH_REQUIRED") {
                    count++
                    entity.copy(syncState = "PENDING", syncErrorMessage = null)
                } else {
                    entity
                }
            }
        return count
    }
}
