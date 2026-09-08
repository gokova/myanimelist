package com.gokova.myanimelist.feature.auth.domain.usecase

import com.gokova.myanimelist.feature.auth.domain.authenticator.MalAuthenticator
import javax.inject.Inject

class LoginWithCodeUseCase
    @Inject
    constructor(
        private val authenticator: MalAuthenticator,
    ) {
        suspend operator fun invoke(code: String): Result<Unit> = authenticator.authenticate(code)
    }
