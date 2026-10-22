package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.repository.IgnoredAnimeRepository
import javax.inject.Inject

class SetAnimeNotInterestedUseCase
    @Inject
    constructor(
        private val repository: IgnoredAnimeRepository,
    ) {
        suspend operator fun invoke(
            animeId: Long,
            title: String,
            titleEnglish: String? = null,
            pictureUrl: String? = null,
        ) {
            repository.setAnimeNotInterested(
                animeId = animeId,
                title = title,
                titleEnglish = titleEnglish,
                pictureUrl = pictureUrl,
            )
        }
    }
