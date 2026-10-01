package com.gokova.myanimelist.feature.profile.data.repository

import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.core.datastore.UserPreferences
import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.domain.repository.ProfileRepository
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.util.executeWithRetry
import com.gokova.myanimelist.feature.profile.data.mapper.toDomain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl
    @Inject
    constructor(
        private val apiService: MalApiService,
        private val userPreferences: UserPreferences,
        private val authPreferences: AuthPreferences,
    ) : ProfileRepository {
        override val userProfile: Flow<UserProfile?> = userPreferences.userProfile

        override suspend fun refreshUserProfile(): Result<UserProfile> =
            try {
                val sessionId =
                    authPreferences.sessionId.first()
                        ?: return Result.failure(IllegalStateException("No active session"))
                val dto = executeWithRetry { apiService.getUserProfile() }
                val profile = dto.toDomain()
                val saved =
                    userPreferences.saveUserProfile(profile, expectedSessionId = sessionId)
                if (saved) {
                    Result.success(profile)
                } else {
                    Result.failure(
                        IllegalStateException("Session cleared or changed during refresh"),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Result.failure(e)
            }

        override suspend fun clearUserProfile() {
            userPreferences.clearUserProfile()
        }
    }
