package com.gokova.myanimelist.core.domain.model

data class PendingAnimeMutation(
    val animeId: Long,
    val status: String? = null,
    val numEpisodesWatched: Int? = null,
    val score: Int? = null,
    val isRewatching: Boolean? = null,
    val syncState: SyncMutationState = SyncMutationState.PENDING,
    val createdAt: Long = 0L,
    val errorMessage: String? = null,
    val conflictDetails: AnimeConflictDetails? = null,
)
