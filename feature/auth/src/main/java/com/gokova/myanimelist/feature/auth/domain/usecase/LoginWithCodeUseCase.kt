package com.gokova.myanimelist.feature.auth.domain.usecase

import com.gokova.myanimelist.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LoginWithCodeUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(
            code: String,
            state: String? = null,
        ): Result<Unit> = authRepository.authenticate(code, state)
    }
