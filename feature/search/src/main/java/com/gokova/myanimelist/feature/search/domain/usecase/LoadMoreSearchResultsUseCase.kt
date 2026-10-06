package com.gokova.myanimelist.feature.search.domain.usecase

import com.gokova.myanimelist.feature.search.domain.model.SearchResultPage
import com.gokova.myanimelist.feature.search.domain.repository.SearchRepository
import javax.inject.Inject

class LoadMoreSearchResultsUseCase
    @Inject
    constructor(
        private val repository: SearchRepository,
    ) {
        suspend operator fun invoke(nextUrl: String): Result<SearchResultPage> =
            repository.loadMore(nextUrl)
    }
