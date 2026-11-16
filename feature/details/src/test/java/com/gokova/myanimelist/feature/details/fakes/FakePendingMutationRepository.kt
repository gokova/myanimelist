package com.gokova.myanimelist.feature.details.fakes

import com.gokova.myanimelist.core.domain.model.ConflictResolution
import com.gokova.myanimelist.core.domain.model.PendingAnimeMutation
import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.repository.PendingMutationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePendingMutationRepository : PendingMutationRepository {
    val pendingMutationsFlow = MutableStateFlow<Map<Long, PendingAnimeMutation>>(emptyMap())

    fun emitMutation(mutation: PendingAnimeMutation) {
        pendingMutationsFlow.value += (mutation.animeId to mutation)
    }

    override fun observePendingMutations(): Flow<List<PendingAnimeMutation>> =
        pendingMutationsFlow.map { it.values.toList() }

    override fun observePendingMutation(animeId: Long): Flow<PendingAnimeMutation?> =
        pendingMutationsFlow.map { it[animeId] }

    override suspend fun getPendingMutations(): List<PendingAnimeMutation> =
        pendingMutationsFlow.value.values.toList()

    override suspend fun getPendingMutation(animeId: Long): PendingAnimeMutation? =
        pendingMutationsFlow.value[animeId]

    override suspend fun saveMutationLocally(
        animeId: Long,
        params: UpdateUserAnimeStatusParams,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun resolveConflict(
        animeId: Long,
        resolution: ConflictResolution,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun discardMutation(animeId: Long): Result<Unit> = Result.success(Unit)

    override suspend fun clearAllPendingMutations(): Result<Unit> {
        pendingMutationsFlow.value = emptyMap()
        return Result.success(Unit)
    }

    override suspend fun getPendingCount(): Int = pendingMutationsFlow.value.size

    override suspend fun syncMutation(animeId: Long): Result<Unit> = Result.success(Unit)

    override suspend fun syncAllPending(): Result<Unit> = Result.success(Unit)

    override suspend fun resetAuthRequiredMutations(): Result<Unit> = Result.success(Unit)
}
