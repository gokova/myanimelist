package com.gokova.myanimelist.feature.mylist.testing

import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SyncStatus
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.repository.MyListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FakeMyListRepository : MyListRepository {
    private val animeListFlow = MutableStateFlow<List<UserAnime>>(emptyList())
    var shouldFailSync = false
    var shouldSkipDueToCooldown = false
    var syncCallCount = 0
    var lastForceFlag: Boolean? = null

    fun setAnimeList(list: List<UserAnime>) {
        animeListFlow.value = list
    }

    override fun observeUserAnimeList(category: ListFilterCategory): Flow<List<UserAnime>> =
        animeListFlow.map { list ->
            if (category.status != null) {
                list.filter { it.userStatus == category.status }
            } else {
                list
            }
        }

    override fun syncUserAnimeList(force: Boolean): Flow<SyncStatus> =
        flow {
            syncCallCount++
            lastForceFlag = force
            if (!force && shouldSkipDueToCooldown) {
                emit(SyncStatus.SkippedCooldown)
                return@flow
            }
            emit(SyncStatus.Started)
            if (shouldFailSync) {
                emit(SyncStatus.Failure(Exception("Network error")))
            } else {
                emit(SyncStatus.Completed)
            }
        }
}
