package com.gokova.myanimelist.core.domain.model

sealed interface ConflictResolution {
    data object KeepDevice : ConflictResolution

    data object UseRemote : ConflictResolution

    data class CustomEdit(
        val params: UpdateUserAnimeStatusParams,
    ) : ConflictResolution
}
