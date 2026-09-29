package com.gokova.myanimelist.feature.details.domain.repository

import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import kotlinx.coroutines.flow.Flow

interface AnimeDetailsRepository {
    fun observeAnimeDetails(animeId: Long): Flow<AnimeDetails?>

    suspend fun fetchFreshAnimeDetails(animeId: Long): Result<AnimeDetails>

    suspend fun addAnimeToPlanToWatch(animeId: Long): Result<Unit>
}
