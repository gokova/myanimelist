package com.gokova.myanimelist.feature.recommendation.data.mapper

import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.RecommendationEntity
import com.gokova.myanimelist.core.database.model.RecommendationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecommendationMapperTest {
    @Test
    fun `toDomain maps both medium and large image urls when present`() {
        val item =
            RecommendationItem(
                recommendation =
                    RecommendationEntity(
                        animeId = 1L,
                        genreScore = 4.5,
                        genreRank = 1,
                        themeScore = 4.0,
                        themeRank = 2,
                        genreMatchPercent = 95,
                        themeMatchPercent = 90,
                    ),
                anime =
                    AnimeEntity(
                        id = 1L,
                        title = "Test Anime",
                        titleEnglish = "Test Anime EN",
                        mainPictureMedium = "https://example.com/medium.jpg",
                        mainPictureLarge = "https://example.com/large.jpg",
                        mediaType = "tv",
                        airingStatus = "finished_airing",
                        numEpisodes = 12,
                        startSeasonYear = 2024,
                        startSeasonSeason = "spring",
                        meanScore = 8.5,
                    ),
            )

        val domain = RecommendationMapper.toDomain(item)

        assertEquals("https://example.com/medium.jpg", domain.thumbnailUrl)
        assertEquals("https://example.com/large.jpg", domain.largeImageUrl)
    }

    @Test
    fun `toDomain falls back to large image when medium is missing`() {
        val item =
            RecommendationItem(
                recommendation =
                    RecommendationEntity(
                        animeId = 1L,
                        genreScore = 4.5,
                        genreRank = 1,
                        themeScore = 4.0,
                        themeRank = 2,
                        genreMatchPercent = 95,
                        themeMatchPercent = 90,
                    ),
                anime =
                    AnimeEntity(
                        id = 1L,
                        title = "Test Anime",
                        titleEnglish = null,
                        mainPictureMedium = null,
                        mainPictureLarge = "https://example.com/large.jpg",
                        mediaType = "tv",
                        airingStatus = "finished_airing",
                        numEpisodes = 12,
                        startSeasonYear = 2024,
                        startSeasonSeason = "spring",
                        meanScore = 8.5,
                    ),
            )

        val domain = RecommendationMapper.toDomain(item)

        assertEquals("https://example.com/large.jpg", domain.thumbnailUrl)
        assertEquals("https://example.com/large.jpg", domain.largeImageUrl)
    }

    @Test
    fun `toDomain handles null image urls gracefully`() {
        val item =
            RecommendationItem(
                recommendation =
                    RecommendationEntity(
                        animeId = 1L,
                        genreScore = 4.5,
                        genreRank = 1,
                        themeScore = 4.0,
                        themeRank = 2,
                        genreMatchPercent = 95,
                        themeMatchPercent = 90,
                    ),
                anime =
                    AnimeEntity(
                        id = 1L,
                        title = "Test Anime",
                        titleEnglish = null,
                        mainPictureMedium = null,
                        mainPictureLarge = null,
                        mediaType = "tv",
                        airingStatus = "finished_airing",
                        numEpisodes = 12,
                        startSeasonYear = 2024,
                        startSeasonSeason = "spring",
                        meanScore = 8.5,
                    ),
            )

        val domain = RecommendationMapper.toDomain(item)

        assertNull(domain.thumbnailUrl)
        assertNull(domain.largeImageUrl)
    }
}
