package com.gokova.myanimelist.feature.recommendation.data.mapper

import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.NewSeasonAnimeEntity
import com.gokova.myanimelist.core.database.model.NewSeasonAnimeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewSeasonMapperTest {
    @Test
    fun `toDomain maps both medium and large image urls when present`() {
        val item =
            NewSeasonAnimeItem(
                newSeason =
                    NewSeasonAnimeEntity(
                        animeId = 2L,
                        parentAnimeId = 1L,
                        relationType = "sequel",
                        relationTypeFormatted = "Sequel",
                    ),
                anime =
                    AnimeEntity(
                        id = 2L,
                        title = "Season 2",
                        titleEnglish = "Season 2 EN",
                        mainPictureMedium = "https://example.com/s2_med.jpg",
                        mainPictureLarge = "https://example.com/s2_large.jpg",
                        mediaType = "tv",
                        airingStatus = "currently_airing",
                        numEpisodes = 24,
                        startSeasonYear = 2026,
                        startSeasonSeason = "summer",
                        meanScore = 8.8,
                    ),
                parentAnime =
                    AnimeEntity(
                        id = 1L,
                        title = "Season 1",
                        titleEnglish = null,
                        mainPictureMedium = null,
                        mainPictureLarge = null,
                        mediaType = "tv",
                        airingStatus = "finished_airing",
                        numEpisodes = 24,
                        startSeasonYear = 2024,
                        startSeasonSeason = "summer",
                        meanScore = 8.2,
                    ),
            )

        val domain = NewSeasonMapper.toDomain(item)

        assertEquals("https://example.com/s2_med.jpg", domain.thumbnailUrl)
        assertEquals("https://example.com/s2_large.jpg", domain.largeImageUrl)
    }

    @Test
    fun `toDomain falls back to large image when medium is missing`() {
        val item =
            NewSeasonAnimeItem(
                newSeason =
                    NewSeasonAnimeEntity(
                        animeId = 2L,
                        parentAnimeId = 1L,
                        relationType = "sequel",
                        relationTypeFormatted = "Sequel",
                    ),
                anime =
                    AnimeEntity(
                        id = 2L,
                        title = "Season 2",
                        titleEnglish = null,
                        mainPictureMedium = null,
                        mainPictureLarge = "https://example.com/s2_large.jpg",
                        mediaType = "tv",
                        airingStatus = "currently_airing",
                        numEpisodes = 24,
                        startSeasonYear = 2026,
                        startSeasonSeason = "summer",
                        meanScore = 8.8,
                    ),
                parentAnime =
                    AnimeEntity(
                        id = 1L,
                        title = "Season 1",
                        titleEnglish = null,
                        mainPictureMedium = null,
                        mainPictureLarge = null,
                        mediaType = "tv",
                        airingStatus = "finished_airing",
                        numEpisodes = 24,
                        startSeasonYear = 2024,
                        startSeasonSeason = "summer",
                        meanScore = 8.2,
                    ),
            )

        val domain = NewSeasonMapper.toDomain(item)

        assertEquals("https://example.com/s2_large.jpg", domain.thumbnailUrl)
        assertEquals("https://example.com/s2_large.jpg", domain.largeImageUrl)
    }

    @Test
    fun `toDomain handles null image urls gracefully`() {
        val item =
            NewSeasonAnimeItem(
                newSeason =
                    NewSeasonAnimeEntity(
                        animeId = 2L,
                        parentAnimeId = 1L,
                        relationType = "sequel",
                        relationTypeFormatted = "Sequel",
                    ),
                anime =
                    AnimeEntity(
                        id = 2L,
                        title = "Season 2",
                        titleEnglish = null,
                        mainPictureMedium = null,
                        mainPictureLarge = null,
                        mediaType = "tv",
                        airingStatus = "currently_airing",
                        numEpisodes = 24,
                        startSeasonYear = 2026,
                        startSeasonSeason = "summer",
                        meanScore = 8.8,
                    ),
                parentAnime =
                    AnimeEntity(
                        id = 1L,
                        title = "Season 1",
                        titleEnglish = null,
                        mainPictureMedium = null,
                        mainPictureLarge = null,
                        mediaType = "tv",
                        airingStatus = "finished_airing",
                        numEpisodes = 24,
                        startSeasonYear = 2024,
                        startSeasonSeason = "summer",
                        meanScore = 8.2,
                    ),
            )

        val domain = NewSeasonMapper.toDomain(item)

        assertNull(domain.thumbnailUrl)
        assertNull(domain.largeImageUrl)
    }
}
