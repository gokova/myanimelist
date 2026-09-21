package com.gokova.myanimelist.feature.recommendation.domain.usecase

import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonState
import com.gokova.myanimelist.feature.recommendation.domain.repository.NewSeasonRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNewSeasonStateUseCase
    @Inject
    constructor(
        private val repository: NewSeasonRepository,
    ) {
        operator fun invoke(): Flow<NewSeasonState> = repository.observeNewSeasonState()
    }
