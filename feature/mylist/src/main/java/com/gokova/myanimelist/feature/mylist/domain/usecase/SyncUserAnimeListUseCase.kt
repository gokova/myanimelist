package com.gokova.myanimelist.feature.mylist.domain.usecase

import com.gokova.myanimelist.feature.mylist.domain.model.SyncStatus
import com.gokova.myanimelist.feature.mylist.domain.repository.MyListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SyncUserAnimeListUseCase
    @Inject
    constructor(
        private val repository: MyListRepository,
    ) {
        operator fun invoke(force: Boolean = false): Flow<SyncStatus> = repository.syncUserAnimeList(force)
    }
