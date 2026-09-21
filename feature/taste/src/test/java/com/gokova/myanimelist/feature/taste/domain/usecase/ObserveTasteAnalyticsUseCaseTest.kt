package com.gokova.myanimelist.feature.taste.domain.usecase

import com.gokova.myanimelist.feature.taste.domain.model.UserAnimeRecord
import com.gokova.myanimelist.feature.taste.domain.repository.TasteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveTasteAnalyticsUseCaseTest {
    private class FakeTasteRepository(
        private val records: List<UserAnimeRecord>,
    ) : TasteRepository {
        override fun observeUserAnimeRecords(): Flow<List<UserAnimeRecord>> = flowOf(records)
    }

    @Test
    fun invoke_whenEmptyRecords_returnsEmptyAnalytics() =
        runTest {
            val useCase = ObserveTasteAnalyticsUseCase(FakeTasteRepository(emptyList()))
            val result = useCase().first()

            assertTrue(result.genres.isEmpty())
            assertTrue(result.themes.isEmpty())
            assertEquals(0, result.totalAnimeCount)
        }

    @Test
    fun invoke_whenOccurrencesLessThanThree_areFilteredOut() =
        runTest {
            val records =
                listOf(
                    createAnimeRecord(1, listOf("Action", "Isekai")),
                    createAnimeRecord(2, listOf("Action", "Isekai")),
                    createAnimeRecord(3, listOf("Comedy")),
                )
            val useCase = ObserveTasteAnalyticsUseCase(FakeTasteRepository(records))
            val result = useCase().first()

            // Both Action (2), Isekai (2), Comedy (1) are < 3
            assertTrue(result.genres.isEmpty())
            assertTrue(result.themes.isEmpty())
            assertEquals(3, result.totalAnimeCount)
        }

    @Test
    fun invoke_whenOccurrencesAtLeastThree_classifiesAndBuildsBubbles() =
        runTest {
            val records =
                listOf(
                    createAnimeRecord(1, listOf("Action", "Shounen"), score = 8),
                    createAnimeRecord(2, listOf("Action", "Shounen"), score = 9),
                    createAnimeRecord(3, listOf("Action", "Shounen"), score = 10),
                    createAnimeRecord(4, listOf("Action", "Romance"), score = 7),
                    createAnimeRecord(5, listOf("Romance")),
                )
            val useCase = ObserveTasteAnalyticsUseCase(FakeTasteRepository(records))
            val result = useCase().first()

            assertEquals(1, result.genres.size)
            val actionBubble = result.genres[0]
            assertEquals("Action", actionBubble.name)
            assertEquals(4, actionBubble.count)
            assertEquals(4, actionBubble.matchingAnime.size)
            assertEquals(8.5, actionBubble.averageScore!!, 0.01)

            assertEquals(1, result.themes.size)
            val shounenBubble = result.themes[0]
            assertEquals("Shounen", shounenBubble.name)
            assertEquals(3, shounenBubble.count)
            assertEquals(3, shounenBubble.matchingAnime.size)
            assertEquals(9.0, shounenBubble.averageScore!!, 0.01)
        }

    private fun createAnimeRecord(
        id: Long,
        genres: List<String>,
        score: Int = 0,
    ) = UserAnimeRecord(
        animeId = id,
        title = "Anime $id",
        thumbnailUrl = null,
        largeImageUrl = null,
        status = "completed",
        userScore = score,
        numEpisodesWatched = 12,
        totalEpisodes = 12,
        genres = genres,
    )
}
