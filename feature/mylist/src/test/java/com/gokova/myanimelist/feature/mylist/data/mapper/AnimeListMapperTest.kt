package com.gokova.myanimelist.feature.mylist.data.mapper

import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.network.model.AlternativeTitlesDto
import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.model.MyListStatusDto
import com.gokova.myanimelist.core.network.model.PictureDto
import com.gokova.myanimelist.core.network.model.StartSeasonDto
import com.gokova.myanimelist.feature.mylist.domain.model.AiringStatus
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnimeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimeListMapperTest {
    @Test
    fun `toAnimeEntity maps DTO correctly extracting English title`() {
        val dto =
            AnimeListEntryDto(
                node =
                    AnimeNodeDto(
                        id = 123L,
                        title = "Shingeki no Kyojin",
                        mainPicture = PictureDto(medium = "med.jpg", large = "large.jpg"),
                        alternativeTitles =
                            AlternativeTitlesDto(
                                en = "Attack on Titan",
                                ja = "進撃の巨人",
                            ),
                        mediaType = "tv",
                        status = "finished_airing",
                        numEpisodes = 25,
                        startSeason = StartSeasonDto(year = 2013, season = "spring"),
                        mean = 8.54,
                    ),
            )

        val entity = AnimeListMapper.toAnimeEntity(dto)

        assertEquals(123L, entity.id)
        assertEquals("Shingeki no Kyojin", entity.title)
        assertEquals("Attack on Titan", entity.titleEnglish)
        assertEquals("med.jpg", entity.mainPictureMedium)
        assertEquals("large.jpg", entity.mainPictureLarge)
        assertEquals("tv", entity.mediaType)
        assertEquals("finished_airing", entity.airingStatus)
        assertEquals(25, entity.numEpisodes)
        assertEquals(2013, entity.startSeasonYear)
        assertEquals("spring", entity.startSeasonSeason)
        assertEquals(8.54, entity.meanScore ?: 0.0, 0.001)
    }

    @Test
    fun `toUserAnimeListEntity maps listStatus correctly`() {
        val dto =
            AnimeListEntryDto(
                node = AnimeNodeDto(id = 123L, title = "Test Anime"),
                listStatus =
                    MyListStatusDto(
                        status = "watching",
                        score = 9,
                        numEpisodesWatched = 12,
                        isRewatching = true,
                        updatedAt = "2023-01-01T00:00:00Z",
                    ),
            )

        val entity = AnimeListMapper.toUserAnimeListEntity(dto)

        assertEquals(123L, entity.animeId)
        assertEquals("watching", entity.status)
        assertEquals(9, entity.score)
        assertEquals(12, entity.numEpisodesWatched)
        assertEquals(true, entity.isRewatching)
        assertEquals("2023-01-01T00:00:00Z", entity.updatedAt)
    }

    @Test
    fun `toDomain maps English title as default and original title as subtitle`() {
        val item =
            UserAnimeListItem(
                anime =
                    AnimeEntity(
                        id = 1L,
                        title = "Boku dake ga Inai Machi",
                        titleEnglish = "ERASED",
                        mainPictureMedium = "m.jpg",
                        mainPictureLarge = "l.jpg",
                        mediaType = "tv",
                        airingStatus = "finished_airing",
                        numEpisodes = 12,
                        startSeasonYear = 2016,
                        startSeasonSeason = "winter",
                        meanScore = 8.3,
                    ),
                userAnime =
                    UserAnimeListEntity(
                        animeId = 1L,
                        status = "completed",
                        score = 10,
                        numEpisodesWatched = 12,
                        updatedAt = "2023-08-01",
                        isRewatching = false,
                    ),
            )

        val domain = AnimeListMapper.toDomain(item)

        assertEquals("ERASED", domain.displayTitle)
        assertEquals("Boku dake ga Inai Machi", domain.subtitleTitle)
        assertEquals(UserAnimeStatus.COMPLETED, domain.userStatus)
        assertEquals(AiringStatus.FINISHED_AIRING, domain.airingStatus)
        assertEquals("2016 Winter", domain.releaseSeason)
        assertEquals("TV", domain.mediaType)
    }

    @Test
    fun `toDomain subtitle is null when English title equals original title`() {
        val item =
            UserAnimeListItem(
                anime =
                    AnimeEntity(
                        id = 2L,
                        title = "One Piece",
                        titleEnglish = "One Piece",
                        mainPictureMedium = null,
                        mainPictureLarge = null,
                        mediaType = null,
                        airingStatus = null,
                        numEpisodes = null,
                        startSeasonYear = null,
                        startSeasonSeason = null,
                        meanScore = null,
                    ),
                userAnime =
                    UserAnimeListEntity(
                        animeId = 2L,
                        status = "watching",
                        score = 9,
                        numEpisodesWatched = 1000,
                        updatedAt = "2023-08-01",
                        isRewatching = false,
                    ),
            )

        val domain = AnimeListMapper.toDomain(item)

        assertEquals("One Piece", domain.displayTitle)
        assertNull(domain.subtitleTitle)
    }
}
