package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.domain.repository.ProfileRepository

class RefreshUserProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): Result<UserProfile> = profileRepository.refreshUserProfile()
}
