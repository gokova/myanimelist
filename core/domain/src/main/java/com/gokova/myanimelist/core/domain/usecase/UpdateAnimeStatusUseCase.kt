package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.model.UpdateUserAnimeStatusParams
import com.gokova.myanimelist.core.domain.model.UserAnimeListUpdate
import com.gokova.myanimelist.core.domain.repository.UserAnimeListStatusRepository
import javax.inject.Inject

class UpdateAnimeStatusUseCase
    @Inject
    constructor(
        private val repository: UserAnimeListStatusRepository,
    ) {
        suspend operator fun invoke(
            animeId: Long,
            params: UpdateUserAnimeStatusParams,
        ): Result<UserAnimeListUpdate> = repository.updateAnimeStatus(animeId, params)
    }
