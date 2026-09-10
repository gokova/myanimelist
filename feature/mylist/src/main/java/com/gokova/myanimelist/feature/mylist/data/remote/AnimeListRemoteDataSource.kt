package com.gokova.myanimelist.feature.mylist.data.remote

import com.gokova.myanimelist.core.network.model.AnimeListEntryDto

interface AnimeListRemoteDataSource {
    suspend fun fetchAllUserAnime(): List<AnimeListEntryDto>
}
