package com.gokova.myanimelist.feature.franchise.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FranchiseNodeTest {
    @Test
    fun `displayTitle prefers English title when present and non-blank`() {
        val node = createNode(title = "Shingeki no Kyojin", titleEnglish = "Attack on Titan")
        assertEquals("Attack on Titan", node.displayTitle)
    }

    @Test
    fun `displayTitle falls back to default title when English title is blank or null`() {
        val nodeWithNull = createNode(title = "Shingeki no Kyojin", titleEnglish = null)
        assertEquals("Shingeki no Kyojin", nodeWithNull.displayTitle)

        val nodeWithBlank = createNode(title = "Shingeki no Kyojin", titleEnglish = "   ")
        assertEquals("Shingeki no Kyojin", nodeWithBlank.displayTitle)
    }

    @Test
    fun `formattedMediaType formats common media types cleanly`() {
        assertEquals("TV", createNode(mediaType = "tv").formattedMediaType)
        assertEquals("Movie", createNode(mediaType = "movie").formattedMediaType)
        assertEquals("OVA", createNode(mediaType = "ova").formattedMediaType)
        assertEquals("ONA", createNode(mediaType = "ona").formattedMediaType)
        assertEquals("Special", createNode(mediaType = "special").formattedMediaType)
        assertEquals("TV Special", createNode(mediaType = "tv_special").formattedMediaType)
        assertEquals("Music", createNode(mediaType = "music").formattedMediaType)
        assertEquals("PV", createNode(mediaType = "pv").formattedMediaType)
        assertEquals("CM", createNode(mediaType = "cm").formattedMediaType)
        assertEquals("Anime", createNode(mediaType = null).formattedMediaType)
    }

    @Test
    fun `formattedMediaType replaces underscores and capitalizes unknown media types`() {
        assertEquals("Side Story", createNode(mediaType = "side_story").formattedMediaType)
    }

    private fun createNode(
        title: String = "Test Anime",
        titleEnglish: String? = null,
        mediaType: String? = "tv",
    ) = FranchiseNode(
        animeId = 1L,
        title = title,
        titleEnglish = titleEnglish,
        posterMedium = null,
        posterLarge = null,
        mediaType = mediaType,
        numEpisodes = 12,
        isInUserList = false,
        userStatus = null,
        episodesWatched = 0,
        isRewatching = false,
        isNotInterested = false,
        isOrigin = false,
    )
}
