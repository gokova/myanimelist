package com.gokova.myanimelist.feature.franchise.domain.repository

import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraph
import kotlinx.coroutines.flow.Flow

interface FranchiseRepository {
    fun observeFranchiseGraph(rootAnimeId: Long): Flow<FranchiseGraph>

    suspend fun incrementWatchedEpisode(animeId: Long): Result<Unit>

    suspend fun unmarkNotInterested(animeId: Long): Result<Unit>
}
