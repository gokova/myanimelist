package com.gokova.myanimelist.feature.auth.domain.usecase

import android.net.Uri
import com.gokova.myanimelist.feature.auth.domain.authenticator.MalAuthenticator
import javax.inject.Inject

class GetAuthUrlUseCase
    @Inject
    constructor(
        private val authenticator: MalAuthenticator,
    ) {
        suspend operator fun invoke(): Uri = authenticator.getAuthorizationUrl()
    }
