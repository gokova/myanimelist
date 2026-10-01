package com.gokova.myanimelist.core.datastore

import com.gokova.myanimelist.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserPreferences {
    val userProfile: Flow<UserProfile?>

    suspend fun saveUserProfile(
        profile: UserProfile,
        expectedSessionId: String? = null,
    ): Boolean

    suspend fun clearUserProfile()
}
