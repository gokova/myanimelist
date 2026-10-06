package com.gokova.myanimelist.core.network.api

import com.gokova.myanimelist.core.network.model.AnimeDetailsDto
import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.core.network.model.GenreDto
import com.gokova.myanimelist.core.network.model.StudioDto
import com.gokova.myanimelist.core.network.model.UserDto
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
    fun `default search limit is 20`() {
        assertEquals(20, MalApiService.DEFAULT_SEARCH_LIMIT)
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

    @Test
    fun `deserializes anime details response with related_anime successfully`() {
        val json = Json { ignoreUnknownKeys = true }
        val details = json.decodeFromString<AnimeDetailsDto>(MAL_ANIME_DETAILS_JSON)

        assertEquals(30230L, details.id)
        assertEquals("Diamond no Ace: Second Season", details.title)
        assertEquals(8.42, details.mean ?: 0.0, 0.001)
        assertEquals(2, details.relatedAnime?.size)

        val firstRelation = details.relatedAnime?.get(0)
        assertEquals(18689L, firstRelation?.node?.id)
        assertEquals("Diamond no Ace", firstRelation?.node?.title)
        assertEquals("prequel", firstRelation?.relationType)
        assertEquals("Prequel", firstRelation?.relationTypeFormatted)

        val nodeDto = details.toAnimeNodeDto()
        assertEquals(30230L, nodeDto.id)
        assertEquals("Diamond no Ace: Second Season", nodeDto.title)
    }

    @Test
    fun `deserializes anime details with recommendations and list status successfully`() {
        val json = Json { ignoreUnknownKeys = true }
        val details =
            json.decodeFromString<AnimeDetailsDto>(
                MAL_ANIME_DETAILS_WITH_RECOMMENDATIONS_JSON,
            )

        assertEquals(30230L, details.id)
        assertEquals(150000, details.numScoringUsers)
        assertEquals("plan_to_watch", details.myListStatus?.status)
        assertEquals(1, details.recommendations?.size)

        val firstRec = details.recommendations?.get(0)
        assertEquals(18689L, firstRec?.node?.id)
        assertEquals("Diamond no Ace", firstRec?.node?.title)
        assertEquals(42, firstRec?.numRecommendations)
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

        private val MAL_ANIME_DETAILS_JSON =
            """
            {
              "id": 30230,
              "title": "Diamond no Ace: Second Season",
              "main_picture": {
                "medium": "https://cdn.myanimelist.net/images/anime/9/74398.jpg",
                "large": "https://cdn.myanimelist.net/images/anime/9/74398l.jpg"
              },
              "mean": 8.42,
              "num_episodes": 51,
              "media_type": "tv",
              "status": "finished_airing",
              "related_anime": [
                {
                  "node": {
                    "id": 18689,
                    "title": "Diamond no Ace",
                    "main_picture": {
                      "medium": "https://cdn.myanimelist.net/images/anime/5/54235.jpg",
                      "large": "https://cdn.myanimelist.net/images/anime/5/54235l.jpg"
                    }
                  },
                  "relation_type": "prequel",
                  "relation_type_formatted": "Prequel"
                },
                {
                  "node": {
                    "id": 34349,
                    "title": "Diamond no Ace: Second Season OVA",
                    "main_picture": {
                      "medium": "https://cdn.myanimelist.net/images/anime/12/83218.jpg",
                      "large": "https://cdn.myanimelist.net/images/anime/12/83218l.jpg"
                    }
                  },
                  "relation_type": "side_story",
                  "relation_type_formatted": "Side story"
                }
              ]
            }
            """.trimIndent()

        private val MAL_ANIME_DETAILS_WITH_RECOMMENDATIONS_JSON =
            """
            {
              "id": 30230,
              "title": "Diamond no Ace: Second Season",
              "num_scoring_users": 150000,
              "my_list_status": {
                "status": "plan_to_watch",
                "score": 0,
                "num_episodes_watched": 0
              },
              "recommendations": [
                {
                  "node": {
                    "id": 18689,
                    "title": "Diamond no Ace"
                  },
                  "num_recommendations": 42
                }
              ]
            }
            """.trimIndent()

        private val MAL_USER_PROFILE_JSON =
            """
            {
              "id": 6548478,
              "name": "rin-0911-3",
              "picture": "https://myanimelist.cdn-dena.com/images/userimages/6548478.jpg",
              "gender": "male",
              "birthday": "1990-01-01",
              "location": "Tokyo",
              "joined_at": "2017-09-11T10:27:46+00:00",
              "anime_statistics": {
                "num_items_watching": 2,
                "num_items_completed": 1,
                "num_items_on_hold": 0,
                "num_items_dropped": 0,
                "num_items_plan_to_watch": 997,
                "num_items": 1000,
                "num_days_watched": 0.09,
                "num_days_watching": 0.04,
                "num_days_completed": 0.05,
                "num_days_on_hold": 0.0,
                "num_days_dropped": 0.0,
                "num_days": 0.09,
                "num_episodes": 8,
                "num_times_rewatched": 0,
                "mean_score": 7.9
              }
            }
            """.trimIndent()
    }

    @Test
    fun `default user profile fields requests anime_statistics and picture`() {
        val fields = MalApiService.DEFAULT_USER_FIELDS
        assertTrue(fields.contains("anime_statistics"))
        assertTrue(fields.contains("picture"))
        assertEquals("@me", MalApiService.USER_ID_ME)
    }

    @Test
    fun `deserializes representative MAL user profile response`() {
        val json = Json { ignoreUnknownKeys = true }
        val user = json.decodeFromString<UserDto>(MAL_USER_PROFILE_JSON)

        assertEquals(6548478L, user.id)
        assertEquals("rin-0911-3", user.name)
        assertEquals(
            "https://myanimelist.cdn-dena.com/images/userimages/6548478.jpg",
            user.picture,
        )
        assertEquals("male", user.gender)
        assertEquals("1990-01-01", user.birthday)
        assertEquals("Tokyo", user.location)
        assertEquals("2017-09-11T10:27:46+00:00", user.joinedAt)

        val stats = user.animeStatistics
        org.junit.Assert.assertNotNull(stats)
        assertEquals(2, stats?.numItemsWatching)
        assertEquals(1, stats?.numItemsCompleted)
        assertEquals(0, stats?.numItemsOnHold)
        assertEquals(0, stats?.numItemsDropped)
        assertEquals(997, stats?.numItemsPlanToWatch)
        assertEquals(1000, stats?.numItems)
        assertEquals(8, stats?.numEpisodes)
        assertEquals(7.9f, stats?.meanScore ?: 0f, 0.01f)
    }

    @Test
    fun `deserializes user profile without optional fields`() {
        val json = Json { ignoreUnknownKeys = true }
        val user = json.decodeFromString<UserDto>("""{"id":123,"name":"test_user"}""")

        assertEquals(123L, user.id)
        assertEquals("test_user", user.name)
        assertNull(user.picture)
        assertNull(user.gender)
        assertNull(user.birthday)
        assertNull(user.location)
        assertNull(user.joinedAt)
        assertNull(user.animeStatistics)
    }
}
