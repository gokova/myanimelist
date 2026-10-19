package com.gokova.myanimelist.core.domain.repository

import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.model.UserAnimeListUpdate

interface UserAnimeListStatusRepository {
    suspend fun updateAnimeStatus(
        animeId: Long,
        params: UpdateUserAnimeStatusParams,
    ): Result<UserAnimeListUpdate>

    suspend fun deleteAnimeFromList(animeId: Long): Result<Unit>
}
