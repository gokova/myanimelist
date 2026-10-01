package com.gokova.myanimelist.core.domain.repository

import com.gokova.myanimelist.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val userProfile: Flow<UserProfile?>

    suspend fun refreshUserProfile(): Result<UserProfile>

    suspend fun clearUserProfile()
}
