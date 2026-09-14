package com.gokova.myanimelist.feature.taste.data.repository

import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.feature.taste.domain.model.UserAnimeRecord
import com.gokova.myanimelist.feature.taste.domain.repository.TasteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TasteRepositoryImpl
    @Inject
    constructor(
        private val userAnimeListDao: UserAnimeListDao,
    ) : TasteRepository {
        override fun observeUserAnimeRecords(): Flow<List<UserAnimeRecord>> =
            userAnimeListDao.observeAllUserAnime().map { listItems ->
                listItems.map { item ->
                    val anime = item.anime
                    val userAnime = item.userAnime
                    UserAnimeRecord(
                        animeId = anime.id,
                        title = anime.titleEnglish ?: anime.title,
                        imageUrl = anime.mainPictureMedium ?: anime.mainPictureLarge,
                        status = userAnime.status,
                        userScore = userAnime.score,
                        numEpisodesWatched = userAnime.numEpisodesWatched,
                        totalEpisodes = anime.numEpisodes,
                        genres = anime.genres?.map { it.name } ?: emptyList(),
                    )
                }
            }
    }
