package com.gokova.myanimelist.feature.auth.domain.usecase

import com.gokova.myanimelist.feature.auth.domain.authenticator.MalAuthenticator
import javax.inject.Inject

class LogoutUseCase
    @Inject
    constructor(
        private val authenticator: MalAuthenticator,
    ) {
        suspend operator fun invoke() {
            authenticator.logout()
        }
    }
