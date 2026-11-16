package com.gokova.myanimelist.core.domain.repository

import com.gokova.myanimelist.core.domain.model.ConflictResolution
import com.gokova.myanimelist.core.domain.model.PendingAnimeMutation
import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface PendingMutationRepository {
    fun observePendingMutations(): Flow<List<PendingAnimeMutation>>

    fun observePendingMutation(animeId: Long): Flow<PendingAnimeMutation?>

    suspend fun getPendingMutations(): List<PendingAnimeMutation>

    suspend fun getPendingMutation(animeId: Long): PendingAnimeMutation?

    suspend fun saveMutationLocally(
        animeId: Long,
        params: UpdateUserAnimeStatusParams,
    ): Result<Unit>

    suspend fun resolveConflict(
        animeId: Long,
        resolution: ConflictResolution,
    ): Result<Unit>

    suspend fun discardMutation(animeId: Long): Result<Unit>

    suspend fun clearAllPendingMutations(): Result<Unit>

    suspend fun getPendingCount(): Int

    suspend fun syncMutation(animeId: Long): Result<Unit>

    suspend fun syncAllPending(): Result<Unit>

    suspend fun resetAuthRequiredMutations(): Result<Unit>
}
