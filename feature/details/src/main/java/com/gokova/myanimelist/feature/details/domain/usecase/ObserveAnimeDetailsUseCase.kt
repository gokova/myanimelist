package com.gokova.myanimelist.feature.details.domain.usecase

import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.domain.repository.AnimeDetailsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAnimeDetailsUseCase
    @Inject
    constructor(
        private val repository: AnimeDetailsRepository,
    ) {
        operator fun invoke(animeId: Long): Flow<AnimeDetails?> =
            repository.observeAnimeDetails(animeId)
    }
