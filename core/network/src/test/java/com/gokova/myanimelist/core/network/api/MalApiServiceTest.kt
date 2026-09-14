package com.gokova.myanimelist.core.network.api

import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.model.GenreDto
import com.gokova.myanimelist.core.network.model.StudioDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MalApiServiceTest {
    @Test
    fun `default anime list fields requests list_status and not my_list_status`() {
        val fields = MalApiService.DEFAULT_ANIME_LIST_FIELDS
        assertTrue("Expected fields to contain list_status", fields.contains("list_status"))
        assertFalse("Fields must not contain my_list_status", fields.contains("my_list_status"))
    }

    @Test
    fun `default anime list fields requests all required catalog metadata`() {
        val fields = MalApiService.DEFAULT_ANIME_LIST_FIELDS
        val expectedFields =
            listOf(
                "genres",
                "studios",
                "source",
                "synopsis",
                "rating",
                "rank",
                "popularity",
                "num_list_users",
                "average_episode_duration",
                "nsfw",
            )
        for (expected in expectedFields) {
            assertTrue("Expected fields to contain $expected", fields.contains(expected))
        }
    }

    @Test
    fun `deserializes representative MAL anime list response with all catalog metadata`() {
        val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

        val response = json.decodeFromString<AnimeListResponseDto>(FULL_MAL_ANIME_LIST_JSON)

        assertEquals(1, response.data.size)
        val entry = response.data[0]
        assertCoreNodeFields(entry.node)
        assertCatalogMetadataFields(entry.node)
        assertListStatusAndPaging(entry, response)
    }

    @Test
    fun `deserializes minimal anime list response with missing optional catalog fields`() {
        val json = Json { ignoreUnknownKeys = true }
        val response = json.decodeFromString<AnimeListResponseDto>(MINIMAL_MAL_ANIME_LIST_JSON)
        val node = response.data[0].node

        assertEquals(999L, node.id)
        assertEquals("Minimal Title", node.title)
        assertNull(node.genres)
        assertNull(node.studios)
        assertNull(node.source)
        assertNull(node.synopsis)
        assertNull(node.rating)
        assertNull(node.rank)
        assertNull(node.popularity)
        assertNull(node.numListUsers)
        assertNull(node.averageEpisodeDuration)
        assertNull(node.nsfw)
        assertNull(response.data[0].listStatus)
        assertNull(response.paging)
    }

    private fun assertCoreNodeFields(node: AnimeNodeDto) {
        assertEquals(16498L, node.id)
        assertEquals("Shingeki no Kyojin", node.title)
        assertEquals(
            "https://cdn.myanimelist.net/images/anime/10/47347.jpg",
            node.mainPicture?.medium,
        )
        assertEquals(
            "https://cdn.myanimelist.net/images/anime/10/47347l.jpg",
            node.mainPicture?.large,
        )
        assertEquals("Attack on Titan", node.alternativeTitles?.en)
        assertEquals("tv", node.mediaType)
        assertEquals("finished_airing", node.status)
        assertEquals(25, node.numEpisodes)
        assertEquals(2013, node.startSeason?.year)
        assertEquals("spring", node.startSeason?.season)
        assertEquals(8.54, node.mean ?: 0.0, 0.001)
    }

    private fun assertCatalogMetadataFields(node: AnimeNodeDto) {
        assertEquals(listOf(GenreDto(1, "Action"), GenreDto(58, "Gore")), node.genres)
        assertEquals(listOf(StudioDto(858, "Wit Studio")), node.studios)
        assertEquals("manga", node.source)
        assertEquals("Centuries ago, mankind was nearly slaughtered...", node.synopsis)
        assertEquals("r", node.rating)
        assertEquals(115, node.rank)
        assertEquals(1, node.popularity)
        assertEquals(3892019, node.numListUsers)
        assertEquals(1452, node.averageEpisodeDuration)
        assertEquals("white", node.nsfw)
    }

    private fun assertListStatusAndPaging(
        entry: AnimeListEntryDto,
        response: AnimeListResponseDto,
    ) {
        assertEquals("completed", entry.listStatus?.status)
        assertEquals(10, entry.listStatus?.score)
        assertEquals(25, entry.listStatus?.numEpisodesWatched)
        assertEquals(false, entry.listStatus?.isRewatching)
        assertEquals("2023-01-01T00:00:00Z", entry.listStatus?.updatedAt)
        assertEquals(
            "https://api.myanimelist.net/v2/users/@me/animelist?offset=10",
            response.paging?.next,
        )
    }

    companion object {
        private val FULL_MAL_ANIME_LIST_JSON =
            """
            {
              "data": [
                {
                  "node": {
                    "id": 16498,
                    "title": "Shingeki no Kyojin",
                    "main_picture": {
                      "medium": "https://cdn.myanimelist.net/images/anime/10/47347.jpg",
                      "large": "https://cdn.myanimelist.net/images/anime/10/47347l.jpg"
                    },
                    "alternative_titles": {
                      "synonyms": ["AoT"],
                      "en": "Attack on Titan",
                      "ja": "\u9032\u6483\u306E\u5DE8\u4EBA"
                    },
                    "media_type": "tv",
                    "status": "finished_airing",
                    "genres": [
                      { "id": 1, "name": "Action" },
                      { "id": 58, "name": "Gore" }
                    ],
                    "num_episodes": 25,
                    "start_season": {
                      "year": 2013,
                      "season": "spring"
                    },
                    "mean": 8.54,
                    "rank": 115,
                    "popularity": 1,
                    "num_list_users": 3892019,
                    "synopsis": "Centuries ago, mankind was nearly slaughtered...",
                    "source": "manga",
                    "average_episode_duration": 1452,
                    "rating": "r",
                    "studios": [
                      { "id": 858, "name": "Wit Studio" }
                    ],
                    "nsfw": "white"
                  },
                  "list_status": {
                    "status": "completed",
                    "score": 10,
                    "num_episodes_watched": 25,
                    "is_rewatching": false,
                    "updated_at": "2023-01-01T00:00:00Z"
                  }
                }
              ],
              "paging": {
                "next": "https://api.myanimelist.net/v2/users/@me/animelist?offset=10"
              }
            }
            """.trimIndent()

        private val MINIMAL_MAL_ANIME_LIST_JSON =
            """
            {
              "data": [
                {
                  "node": {
                    "id": 999,
                    "title": "Minimal Title"
                  }
                }
              ]
            }
            """.trimIndent()
    }
}
