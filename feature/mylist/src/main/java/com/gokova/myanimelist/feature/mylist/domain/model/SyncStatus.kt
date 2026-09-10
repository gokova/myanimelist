package com.gokova.myanimelist.feature.mylist.domain.model

sealed interface SyncStatus {
    data object Started : SyncStatus

    data object Completed : SyncStatus

    data object SkippedCooldown : SyncStatus

    data class Failure(
        val error: Throwable,
    ) : SyncStatus
}
