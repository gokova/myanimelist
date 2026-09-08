package com.gokova.myanimelist.feature.auth.testing

import android.net.Uri
import com.gokova.myanimelist.feature.auth.domain.authenticator.MalAuthenticator

class FakeMalAuthenticator : MalAuthenticator {
    var authenticateCalledWith: String? = null
    var authenticateResult: Result<Unit> = Result.success(Unit)
    var logoutCalled: Boolean = false

    override suspend fun getAuthorizationUrl(): Uri {
        error("Not needed in unit test")
    }

    override suspend fun authenticate(code: String): Result<Unit> {
        authenticateCalledWith = code
        return authenticateResult
    }

    override suspend fun logout() {
        logoutCalled = true
    }
}
