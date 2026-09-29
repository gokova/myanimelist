package com.gokova.myanimelist.feature.details.domain.usecase

import com.gokova.myanimelist.feature.details.domain.repository.AnimeDetailsRepository
import javax.inject.Inject

class AddAnimeToMyListUseCase
    @Inject
    constructor(
        private val repository: AnimeDetailsRepository,
    ) {
        suspend operator fun invoke(animeId: Long): Result<Unit> =
            repository.addAnimeToPlanToWatch(animeId)
    }
