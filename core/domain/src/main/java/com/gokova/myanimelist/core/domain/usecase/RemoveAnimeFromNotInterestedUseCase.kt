package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.repository.IgnoredAnimeRepository
import javax.inject.Inject

class RemoveAnimeFromNotInterestedUseCase
    @Inject
    constructor(
        private val repository: IgnoredAnimeRepository,
    ) {
        suspend operator fun invoke(animeId: Long) {
            repository.removeAnimeFromNotInterested(animeId)
        }
    }
