package com.gokova.myanimelist.feature.details.fakes

import com.gokova.myanimelist.core.domain.model.IgnoredAnime
import com.gokova.myanimelist.core.domain.repository.IgnoredAnimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeIgnoredAnimeRepository : IgnoredAnimeRepository {
    private val ignoredMap = MutableStateFlow<Map<Long, IgnoredAnime>>(emptyMap())

    override fun observeIgnoredAnimeIds(): Flow<List<Long>> = ignoredMap.map { it.keys.toList() }

    override suspend fun getIgnoredAnimeIds(): List<Long> = ignoredMap.value.keys.toList()

    override fun observeIsIgnored(animeId: Long): Flow<Boolean> =
        ignoredMap.map { it.containsKey(animeId) }

    override suspend fun isIgnored(animeId: Long): Boolean = ignoredMap.value.containsKey(animeId)

    override suspend fun setAnimeNotInterested(
        animeId: Long,
        title: String,
        titleEnglish: String?,
        pictureUrl: String?,
    ) {
        ignoredMap.value += (
            animeId to
                IgnoredAnime(
                    animeId = animeId,
                    title = title,
                    titleEnglish = titleEnglish,
                    pictureUrl = pictureUrl,
                    createdAt = System.currentTimeMillis(),
                )
        )
    }

    override suspend fun removeAnimeFromNotInterested(animeId: Long) {
        ignoredMap.value -= animeId
    }

    override fun observeAllIgnoredAnime(): Flow<List<IgnoredAnime>> =
        ignoredMap.map { it.values.toList() }
}
