package com.gokova.myanimelist.feature.search.data.local

import com.gokova.myanimelist.core.database.dao.IgnoredAnimeDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchLocalDataSource
    @Inject
    constructor(
        private val userAnimeListDao: UserAnimeListDao,
        private val ignoredAnimeDao: IgnoredAnimeDao,
    ) {
        suspend fun getUserAnimeList(): List<UserAnimeListItem> = userAnimeListDao.getAllUserAnime()

        suspend fun getIgnoredAnimeIds(): Set<Long> = ignoredAnimeDao.getIgnoredAnimeIds().toSet()
    }
