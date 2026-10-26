package com.gokova.myanimelist.core.domain.model

sealed interface ImportIgnoredAnimeResult {
    data class Success(
        val totalCount: Int,
        val newCount: Int,
        val updatedCount: Int,
    ) : ImportIgnoredAnimeResult

    sealed interface Failure : ImportIgnoredAnimeResult {
        data object EmptyFile : Failure

        data object InvalidFormat : Failure

        data class IoError(
            val message: String? = null,
        ) : Failure
    }
}
