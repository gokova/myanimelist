package com.gokova.myanimelist.feature.taste.domain.repository

import com.gokova.myanimelist.feature.taste.domain.model.UserAnimeRecord
import kotlinx.coroutines.flow.Flow

interface TasteRepository {
    fun observeUserAnimeRecords(): Flow<List<UserAnimeRecord>>
}
