package com.gokova.myanimelist.feature.search.domain.usecase

import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage
import com.gokova.myanimelist.feature.search.domain.repository.SearchRepository
import javax.inject.Inject

class SearchAnimeUseCase
    @Inject
    constructor(
        private val repository: SearchRepository,
    ) {
        suspend operator fun invoke(query: String): Result<SearchResultPage> {
            val trimmed = query.trim()
            if (trimmed.length < MIN_QUERY_LENGTH) {
                return Result.failure(
                    IllegalArgumentException("Query must be at least $MIN_QUERY_LENGTH characters"),
                )
            }
            return repository.searchAnime(trimmed)
        }

        companion object {
            const val MIN_QUERY_LENGTH = 3
        }
    }
