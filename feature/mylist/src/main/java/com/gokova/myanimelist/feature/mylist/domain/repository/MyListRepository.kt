package com.gokova.myanimelist.feature.mylist.domain.repository

import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SyncStatus
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import kotlinx.coroutines.flow.Flow

interface MyListRepository {
    fun observeUserAnimeList(category: ListFilterCategory): Flow<List<UserAnime>>

    fun syncUserAnimeList(force: Boolean = false): Flow<SyncStatus>
}
