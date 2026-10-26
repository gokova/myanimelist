package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.repository.IgnoredAnimeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveIgnoredAnimeCountUseCase
    @Inject
    constructor(
        private val repository: IgnoredAnimeRepository,
    ) {
        operator fun invoke(): Flow<Int> = repository.observeIgnoredAnimeCount()
    }
