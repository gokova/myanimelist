package com.gokova.myanimelist.core.domain.usecase

import javax.inject.Inject

class PendingMutationInteractor
    @Inject
    constructor(
        val observePendingMutations: ObservePendingMutationsUseCase,
        val observePendingMutationById: ObservePendingMutationByIdUseCase,
        val resolveAnimeConflict: ResolveAnimeConflictUseCase,
        val syncPendingMutations: SyncPendingMutationsUseCase,
    )
