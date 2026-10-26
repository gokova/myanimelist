package com.gokova.myanimelist.core.database.repository

import com.gokova.myanimelist.core.database.dao.IgnoredAnimeDao
import com.gokova.myanimelist.core.database.entity.IgnoredAnimeEntity
import com.gokova.myanimelist.core.domain.model.IgnoredAnime
import com.gokova.myanimelist.core.domain.model.ImportIgnoredAnimeResult
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

        override suspend fun getAllIgnoredAnime(): List<IgnoredAnime> =
            ignoredAnimeDao.getAllIgnoredAnime().map { entity ->
                IgnoredAnime(
                    animeId = entity.animeId,
                    title = entity.title,
                    titleEnglish = entity.titleEnglish,
                    pictureUrl = entity.pictureUrl,
                    createdAt = entity.createdAt,
                )
            }

        override fun observeIgnoredAnimeCount(): Flow<Int> =
            ignoredAnimeDao.observeIgnoredAnimeCount()

        override suspend fun upsertIgnoredAnimeList(
            items: List<IgnoredAnime>,
        ): ImportIgnoredAnimeResult {
            if (items.isEmpty()) {
                return ImportIgnoredAnimeResult.Failure.EmptyFile
            }
            val distinctItems = items.distinctBy { it.animeId }
            val existingIds = ignoredAnimeDao.getIgnoredAnimeIds().toSet()
            val newCount = distinctItems.count { it.animeId !in existingIds }
            val updatedCount = distinctItems.count { it.animeId in existingIds }

            val entities =
                distinctItems.map { item ->
                    IgnoredAnimeEntity(
                        animeId = item.animeId,
                        title = item.title,
                        titleEnglish = item.titleEnglish,
                        pictureUrl = item.pictureUrl,
                        createdAt = item.createdAt,
                    )
                }
            ignoredAnimeDao.upsertIgnoredAnimes(entities)

            return ImportIgnoredAnimeResult.Success(
                totalCount = distinctItems.size,
                newCount = newCount,
                updatedCount = updatedCount,
            )
        }
    }
