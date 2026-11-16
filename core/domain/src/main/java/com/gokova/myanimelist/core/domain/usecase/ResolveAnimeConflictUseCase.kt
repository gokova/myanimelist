package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.model.ConflictResolution
import com.gokova.myanimelist.core.domain.repository.PendingMutationRepository
import javax.inject.Inject

class ResolveAnimeConflictUseCase
    @Inject
    constructor(
        private val pendingMutationRepository: PendingMutationRepository,
    ) {
        suspend operator fun invoke(
            animeId: Long,
            resolution: ConflictResolution,
        ): Result<Unit> = pendingMutationRepository.resolveConflict(animeId, resolution)
    }
