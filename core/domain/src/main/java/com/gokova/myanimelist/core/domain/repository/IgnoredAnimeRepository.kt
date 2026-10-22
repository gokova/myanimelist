package com.gokova.myanimelist.core.domain.repository

import com.gokova.myanimelist.core.domain.model.IgnoredAnime
import kotlinx.coroutines.flow.Flow

interface IgnoredAnimeRepository {
    fun observeIgnoredAnimeIds(): Flow<List<Long>>

    suspend fun getIgnoredAnimeIds(): List<Long>

    fun observeIsIgnored(animeId: Long): Flow<Boolean>

    suspend fun isIgnored(animeId: Long): Boolean

    suspend fun setAnimeNotInterested(
        animeId: Long,
        title: String,
        titleEnglish: String? = null,
        pictureUrl: String? = null,
    )

    suspend fun removeAnimeFromNotInterested(animeId: Long)

    fun observeAllIgnoredAnime(): Flow<List<IgnoredAnime>>
}
