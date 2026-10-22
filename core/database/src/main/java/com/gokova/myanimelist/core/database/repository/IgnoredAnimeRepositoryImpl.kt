package com.gokova.myanimelist.core.database.repository

import com.gokova.myanimelist.core.database.dao.IgnoredAnimeDao
import com.gokova.myanimelist.core.database.entity.IgnoredAnimeEntity
import com.gokova.myanimelist.core.domain.model.IgnoredAnime
import com.gokova.myanimelist.core.domain.repository.IgnoredAnimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IgnoredAnimeRepositoryImpl
    @Inject
    constructor(
        private val ignoredAnimeDao: IgnoredAnimeDao,
    ) : IgnoredAnimeRepository {
        override fun observeIgnoredAnimeIds(): Flow<List<Long>> =
            ignoredAnimeDao.observeIgnoredAnimeIds()

        override suspend fun getIgnoredAnimeIds(): List<Long> = ignoredAnimeDao.getIgnoredAnimeIds()

        override fun observeIsIgnored(animeId: Long): Flow<Boolean> =
            ignoredAnimeDao.observeIsIgnored(animeId)

        override suspend fun isIgnored(animeId: Long): Boolean = ignoredAnimeDao.isIgnored(animeId)

        override suspend fun setAnimeNotInterested(
            animeId: Long,
            title: String,
            titleEnglish: String?,
            pictureUrl: String?,
        ) {
            ignoredAnimeDao.upsertIgnoredAnime(
                IgnoredAnimeEntity(
                    animeId = animeId,
                    title = title,
                    titleEnglish = titleEnglish,
                    pictureUrl = pictureUrl,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun removeAnimeFromNotInterested(animeId: Long) {
            ignoredAnimeDao.deleteIgnoredAnime(animeId)
        }

        override fun observeAllIgnoredAnime(): Flow<List<IgnoredAnime>> =
            ignoredAnimeDao.observeAllIgnoredAnime().map { entities ->
                entities.map { entity ->
                    IgnoredAnime(
                        animeId = entity.animeId,
                        title = entity.title,
                        titleEnglish = entity.titleEnglish,
                        pictureUrl = entity.pictureUrl,
                        createdAt = entity.createdAt,
                    )
                }
            }
    }
