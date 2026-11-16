package com.gokova.myanimelist.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gokova.myanimelist.core.database.entity.PendingUserAnimeMutationEntity
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions", "LongParameterList")
interface PendingUserAnimeMutationDao {
    @Query("SELECT * FROM pending_user_anime_mutations ORDER BY created_at ASC")
    fun observeAllPendingMutations(): Flow<List<PendingUserAnimeMutationEntity>>

    @Query("SELECT * FROM pending_user_anime_mutations ORDER BY created_at ASC")
    suspend fun getAllPendingMutations(): List<PendingUserAnimeMutationEntity>

    @Query("SELECT * FROM pending_user_anime_mutations WHERE anime_id = :animeId")
    suspend fun getPendingMutationById(animeId: Long): PendingUserAnimeMutationEntity?

    @Query("SELECT * FROM pending_user_anime_mutations WHERE anime_id = :animeId")
    fun observePendingMutationById(animeId: Long): Flow<PendingUserAnimeMutationEntity?>

    @Upsert
    suspend fun upsertPendingMutation(mutation: PendingUserAnimeMutationEntity)

    @Query(
        """
        UPDATE pending_user_anime_mutations
        SET sync_state = :syncState, sync_error_message = :errorMessage
        WHERE anime_id = :animeId
        """,
    )
    suspend fun updateSyncState(
        animeId: Long,
        syncState: String,
        errorMessage: String? = null,
    ): Int

    @Query(
        """
        UPDATE pending_user_anime_mutations
        SET remote_status = :remoteStatus,
            remote_episodes = :remoteEpisodes,
            remote_score = :remoteScore,
            remote_is_rewatching = :remoteIsRewatching,
            sync_state = :syncState
        WHERE anime_id = :animeId
        """,
    )
    suspend fun updateConflict(
        animeId: Long,
        remoteStatus: String?,
        remoteEpisodes: Int?,
        remoteScore: Int?,
        remoteIsRewatching: Boolean?,
        syncState: String,
    ): Int

    @Query("DELETE FROM pending_user_anime_mutations WHERE anime_id = :animeId")
    suspend fun deletePendingMutation(animeId: Long): Int

    @Query("DELETE FROM pending_user_anime_mutations")
    suspend fun clearAllPendingMutations(): Int

    @Query("SELECT COUNT(*) FROM pending_user_anime_mutations")
    suspend fun getPendingCount(): Int

    @Query(
        """
        UPDATE pending_user_anime_mutations
        SET sync_state = 'PENDING', sync_error_message = null
        WHERE sync_state = 'AUTH_REQUIRED'
        """,
    )
    suspend fun resetAuthRequiredMutations(): Int
}
