package com.gokova.myanimelist.core.domain.model

enum class SyncMutationState {
    PENDING,
    SYNCING,
    FAILED,
    CONFLICT,
    AUTH_REQUIRED,
}
