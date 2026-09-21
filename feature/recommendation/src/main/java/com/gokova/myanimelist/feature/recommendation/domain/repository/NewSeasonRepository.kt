package com.gokova.myanimelist.feature.recommendation.domain.repository

import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonState
import kotlinx.coroutines.flow.Flow

interface NewSeasonRepository {
    fun observeNewSeasons(sortOption: NewSeasonSortOption): Flow<List<NewSeasonAnime>>

    fun observeNewSeasonState(): Flow<NewSeasonState>

    suspend fun scheduleInitialOrPeriodicWork()

    suspend fun triggerImmediateEvaluation()
}
