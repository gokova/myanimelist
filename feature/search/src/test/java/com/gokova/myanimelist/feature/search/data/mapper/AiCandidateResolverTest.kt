package com.gokova.myanimelist.feature.search.data.mapper

import com.gokova.myanimelist.core.ai.model.AiCandidateHint
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.GenreEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.network.model.AlternativeTitlesDto
import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.model.GenreDto
import com.gokova.myanimelist.core.network.model.StartSeasonDto
import com.gokova.myanimelist.feature.search.domain.algorithm.SearchTasteMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AiCandidateResolverTest {
    private lateinit var resolver: AiCandidateResolver

    @Before
    fun setUp() {
        resolver = AiCandidateResolver(SearchTasteMatcher())
    }

    @Test
    fun returnsNullWhenAllEntriesAreIgnoredOrAlreadyResolved() {
        val candidate = AiCandidateHint(title = "Bleach", reason = "Swords")
        val entries = listOf(createEntry(1L, "Bleach"), createEntry(2L, "Bleach TYBW"))

        val result =
            resolver.resolveCandidate(
                candidate = candidate,
                entries = entries,
                userList = emptyList(),
                ignoredIds = setOf(1L),
                alreadyResolvedIds = setOf(2L),
            )

        assertNull(result)
    }

    @Test
    fun resolvesByExactEnglishOrMainTitleMatch() {
        val candidate = AiCandidateHint(title = "Attack on Titan", reason = "Epic action")
        val entries =
            listOf(
                createEntry(10L, "Random Anime"),
                createEntry(
                    id = 11L,
                    title = "Shingeki no Kyojin",
                    englishTitle = "Attack on Titan",
                ),
            )

        val result =
            resolver.resolveCandidate(
                candidate = candidate,
                entries = entries,
                userList = emptyList(),
                ignoredIds = emptySet(),
                alreadyResolvedIds = emptySet(),
            )

        assertNotNull(result)
        assertEquals(11L, result?.id)
        assertEquals("Attack on Titan", result?.englishTitle)
        assertEquals("Epic action", result?.aiReason)
    }

    @Test
    fun resolvesByExpectedYearWhenExactTitleNotFound() {
        val candidate =
            AiCandidateHint(
                title = "Berserk",
                expectedYear = 1997,
                reason = "Classic dark fantasy",
            )
        val entries =
            listOf(
                createEntry(id = 20L, title = "Berserk (2016)", year = 2016),
                createEntry(id = 21L, title = "Kenpuu Denki Berserk", year = 1997),
            )

        val result =
            resolver.resolveCandidate(
                candidate = candidate,
                entries = entries,
                userList = emptyList(),
                ignoredIds = emptySet(),
                alreadyResolvedIds = emptySet(),
            )

        assertNotNull(result)
        assertEquals(21L, result?.id)
        assertEquals("Classic dark fantasy", result?.aiReason)
    }

    @Test
    fun returnsNullWhenNeitherTitleNorYearMatches() {
        val candidate =
            AiCandidateHint(
                title = "Berserk",
                expectedYear = 1997,
                reason = "Classic dark fantasy",
            )
        val entries =
            listOf(
                createEntry(id = 20L, title = "Different Anime", year = 2020),
                createEntry(id = 21L, title = "Another Title", year = 2015),
            )

        val result =
            resolver.resolveCandidate(
                candidate = candidate,
                entries = entries,
                userList = emptyList(),
                ignoredIds = emptySet(),
                alreadyResolvedIds = emptySet(),
            )

        assertNull(result)
    }

    @Test
    fun attachesUserStatusAndCalculatesTasteMatch() {
        val candidate = AiCandidateHint(title = "Steins;Gate", reason = "Time travel")
        val entries =
            listOf(
                createEntry(
                    id = 30L,
                    title = "Steins;Gate",
                    genres = listOf("Sci-Fi"),
                ),
            )
        val userList =
            (1L..5L).map { id ->
                val animeId = if (id == 1L) 30L else id
                UserAnimeListItem(
                    userAnime =
                        UserAnimeListEntity(
                            animeId = animeId,
                            status = "completed",
                            score = 10,
                            numEpisodesWatched = 24,
                            updatedAt = "2026-01-01",
                            isRewatching = false,
                        ),
                    anime =
                        AnimeEntity(
                            id = animeId,
                            title = "Anime $animeId",
                            titleEnglish = "Anime $animeId",
                            mainPictureMedium = null,
                            mainPictureLarge = null,
                            mediaType = "tv",
                            airingStatus = "finished_airing",
                            numEpisodes = 24,
                            startSeasonYear = 2011,
                            startSeasonSeason = "spring",
                            meanScore = 9.1,
                            genres = listOf(GenreEntity(2, "Sci-Fi")),
                        ),
                )
            }

        val result =
            resolver.resolveCandidate(
                candidate = candidate,
                entries = entries,
                userList = userList,
                ignoredIds = emptySet(),
                alreadyResolvedIds = emptySet(),
            )

        assertNotNull(result)
        assertEquals(30L, result?.id)
        assertEquals("completed", result?.userStatus)
        assertNotNull(result?.tasteMatchPercent)
    }

    private fun createEntry(
        id: Long,
        title: String,
        englishTitle: String? = null,
        year: Int? = null,
        genres: List<String> = emptyList(),
    ): AnimeListEntryDto =
        AnimeListEntryDto(
            node =
                AnimeNodeDto(
                    id = id,
                    title = title,
                    alternativeTitles = englishTitle?.let { AlternativeTitlesDto(en = it) },
                    startSeason = year?.let { StartSeasonDto(year = it) },
                    genres = genres.mapIndexed { idx, g -> GenreDto(id = idx, name = g) },
                    mean = if (genres.isNotEmpty()) 9.1 else 8.0,
                    numListUsers = 100000,
                ),
        )
}
