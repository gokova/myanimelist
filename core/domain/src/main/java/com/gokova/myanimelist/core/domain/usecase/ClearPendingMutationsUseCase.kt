package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.repository.PendingMutationRepository
import javax.inject.Inject

class ClearPendingMutationsUseCase
    @Inject
    constructor(
        private val pendingMutationRepository: PendingMutationRepository,
    ) {
        suspend operator fun invoke(): Result<Unit> =
            pendingMutationRepository.clearAllPendingMutations()
    }
