package com.gokova.myanimelist.feature.details.fakes

import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.domain.repository.AnimeDetailsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAnimeDetailsRepository : AnimeDetailsRepository {
    private val detailsFlow = MutableStateFlow<AnimeDetails?>(null)
    var fetchResult: Result<AnimeDetails>? = null
    var addResult: Result<Unit> = Result.success(Unit)
    var addCalledWithId: Long? = null

    fun emitDetails(details: AnimeDetails?) {
        detailsFlow.value = details
    }

    override fun observeAnimeDetails(animeId: Long): Flow<AnimeDetails?> = detailsFlow.asStateFlow()

    override suspend fun fetchFreshAnimeDetails(animeId: Long): Result<AnimeDetails> =
        fetchResult ?: detailsFlow.value?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No anime details available"))

    override suspend fun addAnimeToPlanToWatch(animeId: Long): Result<Unit> {
        addCalledWithId = animeId
        if (addResult.isSuccess) {
            detailsFlow.value =
                detailsFlow.value?.copy(
                    isInUserList = true,
                    userStatus = "plan_to_watch",
                )
        }
        return addResult
    }
}
