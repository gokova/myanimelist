package com.gokova.myanimelist.feature.search.domain.usecase

import com.gokova.myanimelist.feature.search.domain.model.AiSearchResult
import com.gokova.myanimelist.feature.search.domain.repository.SearchRepository
import javax.inject.Inject

class AiSearchAnimeUseCase
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
    ) {
        suspend operator fun invoke(prompt: String): Result<AiSearchResult> {
            val trimmed = prompt.trim()
            if (trimmed.length < MIN_PROMPT_LENGTH) {
                return Result.failure(
                    IllegalArgumentException(
                        "Prompt must be at least $MIN_PROMPT_LENGTH characters",
                    ),
                )
            }
            val sanitized = trimmed.take(MAX_PROMPT_LENGTH)
            return searchRepository.aiSearchAnime(sanitized)
        }

        companion object {
            const val MIN_PROMPT_LENGTH = 5
            const val MAX_PROMPT_LENGTH = 500
        }
    }
