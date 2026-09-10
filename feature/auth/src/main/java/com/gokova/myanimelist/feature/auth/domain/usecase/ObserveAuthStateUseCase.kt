package com.gokova.myanimelist.feature.auth.domain.usecase

import com.gokova.myanimelist.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        operator fun invoke(): Flow<Boolean> = authRepository.isLoggedIn
    }
