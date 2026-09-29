package com.gokova.myanimelist.feature.details.domain.usecase

import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.domain.repository.AnimeDetailsRepository
import javax.inject.Inject

class RefreshAnimeDetailsUseCase
    @Inject
    constructor(
        private val repository: AnimeDetailsRepository,
    ) {
        suspend operator fun invoke(animeId: Long): Result<AnimeDetails> =
            repository.fetchFreshAnimeDetails(animeId)
    }
