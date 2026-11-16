package com.gokova.myanimelist.feature.mylist.testing

import com.gokova.myanimelist.core.domain.model.ConflictResolution
import com.gokova.myanimelist.core.domain.model.PendingAnimeMutation
import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.repository.PendingMutationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException

class FakePendingMutationRepository : PendingMutationRepository {
    val pendingFlow = MutableStateFlow<List<PendingAnimeMutation>>(emptyList())
    var lastResolvedAnimeId: Long? = null
    var lastResolution: ConflictResolution? = null
    var lastDiscardedAnimeId: Long? = null
    var syncAllCalled = false
    var syncMutationId: Long? = null
    var shouldFailSync = false
    var syncAllResult: Result<Unit> = Result.success(Unit)

    fun setPendingMutations(mutations: List<PendingAnimeMutation>) {
        pendingFlow.value = mutations
    }

    override fun observePendingMutations(): Flow<List<PendingAnimeMutation>> = pendingFlow

    override fun observePendingMutation(animeId: Long): Flow<PendingAnimeMutation?> =
        pendingFlow.map { list -> list.find { it.animeId == animeId } }

    override suspend fun getPendingMutations(): List<PendingAnimeMutation> = pendingFlow.value

    override suspend fun getPendingMutation(animeId: Long): PendingAnimeMutation? =
        pendingFlow.value.find { it.animeId == animeId }

    override suspend fun saveMutationLocally(
        animeId: Long,
        params: UpdateUserAnimeStatusParams,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun resolveConflict(
        animeId: Long,
        resolution: ConflictResolution,
    ): Result<Unit> {
        lastResolvedAnimeId = animeId
        lastResolution = resolution
        return Result.success(Unit)
    }

    override suspend fun discardMutation(animeId: Long): Result<Unit> {
        lastDiscardedAnimeId = animeId
        return Result.success(Unit)
    }

    override suspend fun clearAllPendingMutations(): Result<Unit> {
        pendingFlow.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun getPendingCount(): Int = pendingFlow.value.size

    override suspend fun syncMutation(animeId: Long): Result<Unit> {
        syncMutationId = animeId
        return if (shouldFailSync) {
            Result.failure(IOException("Sync failed"))
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun syncAllPending(): Result<Unit> {
        syncAllCalled = true
        return syncAllResult
    }

    override suspend fun resetAuthRequiredMutations(): Result<Unit> = Result.success(Unit)
}
