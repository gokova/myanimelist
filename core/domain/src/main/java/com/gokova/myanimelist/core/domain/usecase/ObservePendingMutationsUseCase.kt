package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.model.PendingAnimeMutation
import com.gokova.myanimelist.core.domain.repository.PendingMutationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePendingMutationsUseCase
    @Inject
    constructor(
        private val pendingMutationRepository: PendingMutationRepository,
    ) {
        operator fun invoke(): Flow<List<PendingAnimeMutation>> =
            pendingMutationRepository.observePendingMutations()
    }
