package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.repository.UserAnimeListStatusRepository
import javax.inject.Inject

class DeleteAnimeFromListUseCase
    @Inject
    constructor(
        private val repository: UserAnimeListStatusRepository,
    ) {
        suspend operator fun invoke(animeId: Long): Result<Unit> =
            repository.deleteAnimeFromList(animeId)
    }
