package com.gokova.myanimelist.feature.search.data.mapper

import com.gokova.myanimelist.core.network.model.AlternativeTitlesDto
import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.model.GenreDto
import com.gokova.myanimelist.core.network.model.MyListStatusDto
import com.gokova.myanimelist.core.network.model.PictureDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchMapperTest {
    @Test
    fun mapsBasicFieldsCorrectly() {
        val entry =
            AnimeListEntryDto(
                node =
                    AnimeNodeDto(
                        id = 100L,
                        title = "Fullmetal Alchemist",
                        mainPicture = PictureDto(medium = "med.jpg", large = "large.jpg"),
                        alternativeTitles =
                            AlternativeTitlesDto(
                                en = "Fullmetal Alchemist: Brotherhood",
                            ),
                        mediaType = "tv",
                        numEpisodes = 64,
                        mean = 9.1,
                        genres = listOf(GenreDto(1, "Action"), GenreDto(2, "Adventure")),
                    ),
            )

        val result = SearchMapper.toDomain(entry, localUserStatus = null, tasteMatchPercent = 95)

        assertEquals(100L, result.id)
        assertEquals("Fullmetal Alchemist", result.title)
        assertEquals("Fullmetal Alchemist: Brotherhood", result.englishTitle)
        assertEquals("Fullmetal Alchemist: Brotherhood", result.displayTitle)
        assertEquals("Fullmetal Alchemist", result.subtitleTitle)
        assertEquals("large.jpg", result.thumbnailUrl)
        assertEquals("tv", result.mediaType)
        assertEquals(64, result.numEpisodes)
        assertEquals(9.1, result.meanScore!!, 0.001)
        assertEquals(listOf("Action", "Adventure"), result.genres)
        assertNull(result.userStatus)
        assertEquals(95, result.tasteMatchPercent)
    }

    @Test
    fun handlesMissingOrIdenticalEnglishTitle() {
        val entryIdentical =
            AnimeListEntryDto(
                node =
                    AnimeNodeDto(
                        id = 101L,
                        title = "Bleach",
                        alternativeTitles = AlternativeTitlesDto(en = "Bleach"),
                    ),
            )
        val entryNull =
            AnimeListEntryDto(
                node =
                    AnimeNodeDto(
                        id = 102L,
                        title = "Naruto",
                        alternativeTitles = null,
                    ),
            )

        val resultIdentical =
            SearchMapper.toDomain(entryIdentical, localUserStatus = null, tasteMatchPercent = null)
        val resultNull =
            SearchMapper.toDomain(entryNull, localUserStatus = null, tasteMatchPercent = null)

        assertEquals("Bleach", resultIdentical.displayTitle)
        assertNull(resultIdentical.subtitleTitle)

        assertEquals("Naruto", resultNull.displayTitle)
        assertNull(resultNull.subtitleTitle)
    }

    @Test
    fun prefersLocalUserStatusOverRemoteListStatus() {
        val entry =
            AnimeListEntryDto(
                node = AnimeNodeDto(id = 200L, title = "Steins;Gate"),
                listStatus = MyListStatusDto(status = "plan_to_watch"),
            )

        val result =
            SearchMapper.toDomain(
                dto = entry,
                localUserStatus = "watching",
                tasteMatchPercent = null,
            )

        assertEquals("watching", result.userStatus)
    }

    @Test
    fun fallsBackToRemoteListStatusWhenLocalStatusIsNull() {
        val entry =
            AnimeListEntryDto(
                node = AnimeNodeDto(id = 201L, title = "Steins;Gate 0"),
                listStatus = MyListStatusDto(status = "completed"),
            )

        val result =
            SearchMapper.toDomain(
                dto = entry,
                localUserStatus = null,
                tasteMatchPercent = null,
            )

        assertEquals("completed", result.userStatus)
    }

    @Test
    fun selectsMediumPictureWhenLargeIsNull() {
        val entry =
            AnimeListEntryDto(
                node =
                    AnimeNodeDto(
                        id = 300L,
                        title = "Clannad",
                        mainPicture = PictureDto(medium = "med_only.jpg", large = null),
                    ),
            )

        val result = SearchMapper.toDomain(entry, localUserStatus = null, tasteMatchPercent = null)

        assertEquals("med_only.jpg", result.thumbnailUrl)
    }
}
