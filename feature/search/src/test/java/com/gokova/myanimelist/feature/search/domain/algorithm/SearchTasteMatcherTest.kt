package com.gokova.myanimelist.feature.search.domain.algorithm

import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.GenreEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchTasteMatcherTest {
    private lateinit var matcher: SearchTasteMatcher

    @Before
    fun setUp() {
        matcher = SearchTasteMatcher()
    }

    @Test
    fun returnsNullWhenUserListHasFewerThanMinAnimeCount() {
        val userList = (1L..4L).map { id -> createTestItem(id, listOf("Action"), score = 8) }
        val result =
            matcher.calculateMatch(
                candidateGenres = listOf("Action"),
                candidateMeanScore = 8.0,
                candidateNumListUsers = 5000,
                userList = userList,
            )
        assertNull(result)
    }

    @Test
    fun returnsNullWhenCandidateGenresIsEmpty() {
        val userList = (1L..5L).map { id -> createTestItem(id, listOf("Action"), score = 8) }
        val result =
            matcher.calculateMatch(
                candidateGenres = emptyList(),
                candidateMeanScore = 8.0,
                candidateNumListUsers = 5000,
                userList = userList,
            )
        assertNull(result)
    }

    @Test
    fun returnsNullWhenNoGenresOrThemesMatch() {
        val userList = (1L..5L).map { id -> createTestItem(id, listOf("Action"), score = 8) }
        val result =
            matcher.calculateMatch(
                candidateGenres = listOf("Romance"),
                candidateMeanScore = 8.0,
                candidateNumListUsers = 5000,
                userList = userList,
            )
        assertNull(result)
    }

    @Test
    fun returnsCalibratedMatchPercentWhenUserListHasMatchingGenres() {
        val userList = (1L..5L).map { id -> createTestItem(id, listOf("Action"), score = 8) }
        val result =
            matcher.calculateMatch(
                candidateGenres = listOf("Action"),
                candidateMeanScore = 8.5,
                candidateNumListUsers = 50000,
                userList = userList,
            )
        assertNotNull(result)
        assertTrue(result!! in 10..99)
    }

    @Test
    fun returnsHigherScoreForHigherRatedAnime() {
        val userList = (1L..5L).map { id -> createTestItem(id, listOf("Sci-Fi"), score = 9) }
        val lowResult =
            matcher.calculateMatch(
                candidateGenres = listOf("Sci-Fi"),
                candidateMeanScore = 4.0,
                candidateNumListUsers = 1000,
                userList = userList,
            )
        val highResult =
            matcher.calculateMatch(
                candidateGenres = listOf("Sci-Fi"),
                candidateMeanScore = 9.0,
                candidateNumListUsers = 50000,
                userList = userList,
            )
        assertNotNull(lowResult)
        assertNotNull(highResult)
        assertTrue(highResult!! >= lowResult!!)
    }

    private fun createTestItem(
        id: Long,
        genres: List<String>,
        score: Int,
    ): UserAnimeListItem =
        UserAnimeListItem(
            userAnime =
                UserAnimeListEntity(
                    animeId = id,
                    status = "completed",
                    score = score,
                    numEpisodesWatched = 12,
                    updatedAt = "2026-01-01",
                    isRewatching = false,
                ),
            anime =
                AnimeEntity(
                    id = id,
                    title = "Anime $id",
                    titleEnglish = "Anime $id",
                    mainPictureMedium = null,
                    mainPictureLarge = null,
                    mediaType = "tv",
                    airingStatus = "finished_airing",
                    numEpisodes = 12,
                    startSeasonYear = 2024,
                    startSeasonSeason = "spring",
                    meanScore = 7.5,
                    genres = genres.mapIndexed { index, name -> GenreEntity(index, name) },
                ),
        )
}
