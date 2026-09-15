package com.gokova.myanimelist.feature.mylist.domain.usecase

import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.feature.mylist.domain.model.SyncStatus
import com.gokova.myanimelist.feature.mylist.domain.repository.MyListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class SyncUserAnimeListUseCase
    @Inject
    constructor(
        private val repository: MyListRepository,
    ) {
        operator fun invoke(force: Boolean = false): Flow<SyncStatus> {
            AppLog.domain.d { "SyncUserAnimeListUseCase invoked (force=$force)" }
            return repository.syncUserAnimeList(force).onEach { status ->
                AppLog.domain.d { "SyncUserAnimeListUseCase emitted status: $status" }
            }
        }
    }
