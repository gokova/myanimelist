package com.gokova.myanimelist.feature.mylist.data.mapper

import com.gokova.myanimelist.core.database.entity.PendingUserAnimeMutationEntity
import com.gokova.myanimelist.core.domain.model.AnimeConflictDetails
import com.gokova.myanimelist.core.domain.model.AnimeConflictValues
import com.gokova.myanimelist.core.domain.model.PendingAnimeMutation
import com.gokova.myanimelist.core.domain.model.SyncMutationState

object PendingMutationMapper {
    fun toDomain(entity: PendingUserAnimeMutationEntity): PendingAnimeMutation {
        val syncState =
            when (entity.syncState) {
                PendingUserAnimeMutationEntity.SYNC_STATE_SYNCING -> SyncMutationState.SYNCING
                PendingUserAnimeMutationEntity.SYNC_STATE_FAILED -> SyncMutationState.FAILED
                PendingUserAnimeMutationEntity.SYNC_STATE_CONFLICT -> SyncMutationState.CONFLICT
                PendingUserAnimeMutationEntity.SYNC_STATE_AUTH_REQUIRED ->
                    SyncMutationState.AUTH_REQUIRED
                else -> SyncMutationState.PENDING
            }

        val conflictDetails =
            if (syncState == SyncMutationState.CONFLICT) {
                AnimeConflictDetails(
                    animeId = entity.animeId,
                    baseline =
                        AnimeConflictValues(
                            episodesWatched = entity.baselineEpisodes,
                            status = entity.baselineStatus,
                            score = entity.baselineScore,
                            isRewatching = entity.baselineIsRewatching,
                        ),
                    device =
                        AnimeConflictValues(
                            episodesWatched = entity.numEpisodesWatched,
                            status = entity.status,
                            score = entity.score,
                            isRewatching = entity.isRewatching,
                        ),
                    remote =
                        AnimeConflictValues(
                            episodesWatched = entity.remoteEpisodes,
                            status = entity.remoteStatus,
                            score = entity.remoteScore,
                            isRewatching = entity.remoteIsRewatching,
                        ),
                )
            } else {
                null
            }

        return PendingAnimeMutation(
            animeId = entity.animeId,
            status = entity.status,
            numEpisodesWatched = entity.numEpisodesWatched,
            score = entity.score,
            isRewatching = entity.isRewatching,
            syncState = syncState,
            createdAt = entity.createdAt,
            errorMessage = entity.syncErrorMessage,
            conflictDetails = conflictDetails,
        )
    }
}
