package com.gokova.myanimelist.feature.details.data.mapper

import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.GenreEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.network.model.AlternativeTitlesDto
import com.gokova.myanimelist.core.network.model.AnimeDetailsDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.model.AnimeRecommendationEdgeDto
import com.gokova.myanimelist.core.network.model.GenreDto
import com.gokova.myanimelist.core.network.model.MyListStatusDto
import com.gokova.myanimelist.core.network.model.PictureDto
import com.gokova.myanimelist.core.network.model.RelatedAnimeEdgeDto
import com.gokova.myanimelist.core.network.model.StartSeasonDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimeDetailsMapperTest {
    @Test
    fun `toDomain with distinct English title assigns display and subtitle correctly`() {
        val entity =
            AnimeEntity(
                id = 1L,
                title = "Shingeki no Kyojin",
                titleEnglish = "Attack on Titan",
                mainPictureMedium = "med.jpg",
                mainPictureLarge = "large.jpg",
                mediaType = "tv",
                airingStatus = "finished_airing",
                numEpisodes = 25,
                startSeasonYear = 2013,
                startSeasonSeason = "spring",
                meanScore = 8.54,
                genres = listOf(GenreEntity(1, "Action"), GenreEntity(58, "Gore")),
            )
        val userAnime =
            UserAnimeListEntity(
                animeId = 1L,
                status = "completed",
                score = 9,
                numEpisodesWatched = 25,
                updatedAt = "2024-01-01",
                isRewatching = false,
            )

        val domain = AnimeDetailsMapper.toDomain(entity, userAnime)

        assertEquals("Attack on Titan", domain.displayTitle)
        assertEquals("Shingeki no Kyojin", domain.subtitleTitle)
        assertEquals("completed", domain.userStatus)
        assertEquals(9, domain.userScore)
        assertTrue(domain.isInUserList)
        assertEquals(listOf("Action"), domain.genres)
        assertEquals(listOf("Gore"), domain.themes)
        assertEquals("2013 Spring", domain.season)
    }

    @Test
    fun `toDomain without English title falls back to original title and null subtitle`() {
        val entity =
            AnimeEntity(
                id = 2L,
                title = "Original Title Only",
                titleEnglish = null,
                mainPictureMedium = null,
                mainPictureLarge = null,
                mediaType = "movie",
                airingStatus = "finished_airing",
                numEpisodes = 1,
                startSeasonYear = 2020,
                startSeasonSeason = null,
                meanScore = 7.5,
            )

        val domain = AnimeDetailsMapper.toDomain(entity, null)

        assertEquals("Original Title Only", domain.displayTitle)
        assertNull(domain.subtitleTitle)
        assertFalse(domain.isInUserList)
        assertNull(domain.userStatus)
        assertNull(domain.userScore)
        assertEquals("2020", domain.season)
    }

    @Test
    fun `toDomain from AnimeDetailsDto maps related anime and recommendations capping at 10`() {
        val recs =
            (1..15).map { index ->
                AnimeRecommendationEdgeDto(
                    node = AnimeNodeDto(id = index.toLong(), title = "Rec Anime $index"),
                    numRecommendations = index * 2,
                )
            }
        val relations =
            listOf(
                RelatedAnimeEdgeDto(
                    node = AnimeNodeDto(id = 99L, title = "Sequel Anime"),
                    relationType = "sequel",
                    relationTypeFormatted = "Sequel",
                ),
            )
        val dto =
            AnimeDetailsDto(
                id = 100L,
                title = "Main Anime",
                startSeason = StartSeasonDto(year = 2024, season = "fall"),
                genres = listOf(GenreDto(1, "Action"), GenreDto(2, "Military")),
                myListStatus = MyListStatusDto(status = "plan_to_watch", score = 0),
                relatedAnime = relations,
                recommendations = recs,
            )

        val domain = AnimeDetailsMapper.toDomain(dto, null)

        assertEquals("Main Anime", domain.displayTitle)
        assertEquals(1, domain.relatedAnime.size)
        assertEquals("Sequel", domain.relatedAnime[0].relationTypeFormatted)
        assertEquals(10, domain.recommendations.size)
        assertEquals("2024 Fall", domain.season)
        assertTrue(domain.isInUserList)
        assertEquals("plan_to_watch", domain.userStatus)
    }

    @Test
    fun `toAnimeEntity converts AnimeDetailsDto correctly`() {
        val dto =
            AnimeDetailsDto(
                id = 10L,
                title = "Test Title",
                alternativeTitles = AlternativeTitlesDto(en = "Test English"),
                mainPicture = PictureDto(medium = "m.jpg", large = "l.jpg"),
                mediaType = "tv",
                status = "currently_airing",
                numEpisodes = 12,
                mean = 8.1,
            )

        val entity = AnimeDetailsMapper.toAnimeEntity(dto)

        assertEquals(10L, entity.id)
        assertEquals("Test Title", entity.title)
        assertEquals("Test English", entity.titleEnglish)
        assertEquals("m.jpg", entity.mainPictureMedium)
        assertEquals("l.jpg", entity.mainPictureLarge)
        assertEquals("tv", entity.mediaType)
        assertEquals("currently_airing", entity.airingStatus)
        assertEquals(12, entity.numEpisodes)
        assertEquals(8.1, entity.meanScore ?: 0.0, 0.001)
    }

    @Test
    fun `toDomain with matching English title uses Japanese title as subtitle`() {
        val dto =
            AnimeDetailsDto(
                id = 874L,
                title = "Digimon Tamers",
                alternativeTitles =
                    AlternativeTitlesDto(
                        en = "Digimon Tamers",
                        ja = "デジモンテイマーズ",
                    ),
                numScoringUsers = 131462,
            )

        val domain = AnimeDetailsMapper.toDomain(dto, null)

        assertEquals("Digimon Tamers", domain.displayTitle)
        assertEquals("デジモンテイマーズ", domain.subtitleTitle)
        assertEquals("デジモンテイマーズ", domain.originalTitle)
        assertEquals(131462, domain.numScoringUsers)
    }
}
