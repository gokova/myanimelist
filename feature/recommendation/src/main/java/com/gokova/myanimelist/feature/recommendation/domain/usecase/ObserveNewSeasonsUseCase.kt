package com.gokova.myanimelist.feature.recommendation.domain.usecase

import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.repository.NewSeasonRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNewSeasonsUseCase
    @Inject
    constructor(
        private val repository: NewSeasonRepository,
    ) {
        operator fun invoke(sortOption: NewSeasonSortOption): Flow<List<NewSeasonAnime>> =
            repository.observeNewSeasons(sortOption)
    }
