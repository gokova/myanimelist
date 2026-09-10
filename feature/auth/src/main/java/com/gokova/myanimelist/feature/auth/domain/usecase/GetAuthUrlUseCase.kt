package com.gokova.myanimelist.feature.auth.domain.usecase

import com.gokova.myanimelist.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class GetAuthUrlUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(): String = authRepository.getAuthorizationUrl()
    }
