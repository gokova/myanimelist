package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(): Flow<UserProfile?> = profileRepository.userProfile
}
