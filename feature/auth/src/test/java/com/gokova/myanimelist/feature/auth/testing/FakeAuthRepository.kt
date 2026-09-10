package com.gokova.myanimelist.feature.auth.testing

import com.gokova.myanimelist.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository : AuthRepository {
    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()

    var authUrlToReturn: String = "https://myanimelist.net/v1/oauth2/authorize?test=true"
    var authenticateCalledWith: String? = null
    var authenticateCalledWithState: String? = null
    var authenticateResult: Result<Unit> = Result.success(Unit)
    var logoutCalled: Boolean = false

    override suspend fun getAuthorizationUrl(): String = authUrlToReturn

    override suspend fun authenticate(
        code: String,
        state: String?,
    ): Result<Unit> {
        authenticateCalledWith = code
        authenticateCalledWithState = state
        if (authenticateResult.isSuccess) {
            _isLoggedIn.value = true
        }
        return authenticateResult
    }

    override suspend fun logout() {
        logoutCalled = true
        _isLoggedIn.value = false
    }

    fun setLoggedIn(loggedIn: Boolean) {
        _isLoggedIn.value = loggedIn
    }
}
