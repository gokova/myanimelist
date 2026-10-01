package com.gokova.myanimelist.feature.profile.data.mapper

import com.gokova.myanimelist.core.network.model.UserAnimeStatisticsDto
import com.gokova.myanimelist.core.network.model.UserDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileMapperTest {
    @Test
    fun `maps full UserDto to UserProfile`() {
        val dto =
            UserDto(
                id = 42L,
                name = "test_user",
                picture = "https://example.com/pic.jpg",
                gender = "female",
                birthday = "1995-05-05",
                location = "Kyoto",
                joinedAt = "2020-01-01T12:00:00+00:00",
                animeStatistics =
                    UserAnimeStatisticsDto(
                        numItemsWatching = 3,
                        numItemsCompleted = 50,
                        numItemsOnHold = 2,
                        numItemsDropped = 1,
                        numItemsPlanToWatch = 20,
                        numItems = 76,
                        numDaysWatched = 15.5f,
                        numDaysWatching = 1.0f,
                        numDaysCompleted = 14.0f,
                        numDaysOnHold = 0.3f,
                        numDaysDropped = 0.2f,
                        numDays = 15.5f,
                        numEpisodes = 500,
                        numTimesRewatched = 4,
                        meanScore = 8.5f,
                    ),
            )

        val domain = dto.toDomain()

        assertEquals(42L, domain.id)
        assertEquals("test_user", domain.name)
        assertEquals("https://example.com/pic.jpg", domain.pictureUrl)
        assertEquals("female", domain.gender)
        assertEquals("1995-05-05", domain.birthday)
        assertEquals("Kyoto", domain.location)
        assertEquals("2020-01-01T12:00:00+00:00", domain.joinedAt)

        val stats = domain.statistics
        assertNotNull(stats)
        assertEquals(3, stats?.numItemsWatching)
        assertEquals(50, stats?.numItemsCompleted)
        assertEquals(2, stats?.numItemsOnHold)
        assertEquals(1, stats?.numItemsDropped)
        assertEquals(20, stats?.numItemsPlanToWatch)
        assertEquals(76, stats?.numItems)
        assertEquals(15.5f, stats?.numDaysWatched ?: 0f, 0.01f)
        assertEquals(500, stats?.numEpisodes)
        assertEquals(4, stats?.numTimesRewatched)
        assertEquals(8.5f, stats?.meanScore ?: 0f, 0.01f)
    }

    @Test
    fun `maps minimal UserDto with null statistics`() {
        val dto =
            UserDto(
                id = 100L,
                name = "minimal",
            )

        val domain = dto.toDomain()

        assertEquals(100L, domain.id)
        assertEquals("minimal", domain.name)
        assertNull(domain.pictureUrl)
        assertNull(domain.statistics)
    }
}
