package com.gokova.myanimelist.feature.mylist.domain.usecase

import com.gokova.myanimelist.feature.mylist.domain.model.AiringStatus
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SortOption
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnimeStatus
import com.gokova.myanimelist.feature.mylist.testing.FakeMyListRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ObserveUserAnimeListUseCaseTest {
    private lateinit var repository: FakeMyListRepository
    private lateinit var useCase: ObserveUserAnimeListUseCase

    @Before
    fun setUp() {
        repository = FakeMyListRepository()
        useCase = ObserveUserAnimeListUseCase(repository)
    }

    @Test
    fun `observing category filters items correctly`() =
        runTest {
            val anime1 = createAnime(1L, "Anime 1", UserAnimeStatus.WATCHING, 8)
            val anime2 = createAnime(2L, "Anime 2", UserAnimeStatus.COMPLETED, 10)
            repository.setAnimeList(listOf(anime1, anime2))

            val watchingList = useCase(ListFilterCategory.WATCHING, SortOption.SCORE_DESC).first()
            assertEquals(1, watchingList.size)
            assertEquals("Anime 1", watchingList[0].displayTitle)

            val completedList = useCase(ListFilterCategory.COMPLETED, SortOption.SCORE_DESC).first()
            assertEquals(1, completedList.size)
            assertEquals("Anime 2", completedList[0].displayTitle)

            val allList = useCase(ListFilterCategory.ALL, SortOption.SCORE_DESC).first()
            assertEquals(2, allList.size)
        }

    @Test
    fun `sorting by title sorts alphabetically`() =
        runTest {
            val animeB = createAnime(1L, "Bleach", UserAnimeStatus.WATCHING, 8)
            val animeA = createAnime(2L, "Attack on Titan", UserAnimeStatus.WATCHING, 10)
            repository.setAnimeList(listOf(animeB, animeA))

            val sortedList = useCase(ListFilterCategory.ALL, SortOption.TITLE_ASC).first()
            assertEquals("Attack on Titan", sortedList[0].displayTitle)
            assertEquals("Bleach", sortedList[1].displayTitle)
        }

    @Test
    fun `sorting by score sorts descending with title tie breaking`() =
        runTest {
            val anime8 = createAnime(1L, "Naruto", UserAnimeStatus.WATCHING, 8)
            val anime10B = createAnime(2L, "Death Note", UserAnimeStatus.WATCHING, 10)
            val anime10A = createAnime(3L, "Attack on Titan", UserAnimeStatus.WATCHING, 10)
            repository.setAnimeList(listOf(anime8, anime10B, anime10A))

            val sortedList = useCase(ListFilterCategory.ALL, SortOption.SCORE_DESC).first()
            assertEquals("Attack on Titan", sortedList[0].displayTitle)
            assertEquals("Death Note", sortedList[1].displayTitle)
            assertEquals("Naruto", sortedList[2].displayTitle)
        }

    @Test
    fun `sorting by updated at sorts descending`() =
        runTest {
            val animeOld =
                createAnime(1L, "Old Anime", UserAnimeStatus.WATCHING, 8, updatedAt = "2022-01-01")
            val animeNew =
                createAnime(2L, "New Anime", UserAnimeStatus.WATCHING, 8, updatedAt = "2023-05-15")
            repository.setAnimeList(listOf(animeOld, animeNew))

            val sortedList = useCase(ListFilterCategory.ALL, SortOption.UPDATED_AT_DESC).first()
            assertEquals("New Anime", sortedList[0].displayTitle)
            assertEquals("Old Anime", sortedList[1].displayTitle)
        }

    @Test
    fun `sorting by updated at parses ISO timestamps across different timezone offsets`() =
        runTest {
            // 2023-05-15T10:00:00+09:00 is 01:00 UTC
            val animeTokyo =
                createAnime(
                    id = 1L,
                    title = "Tokyo Anime",
                    status = UserAnimeStatus.WATCHING,
                    score = 8,
                    updatedAt = "2023-05-15T10:00:00+09:00",
                )
            // 2023-05-15T08:00:00Z is 08:00 UTC (7 hours newer than Tokyo anime)
            val animeUtc =
                createAnime(
                    id = 2L,
                    title = "UTC Anime",
                    status = UserAnimeStatus.WATCHING,
                    score = 8,
                    updatedAt = "2023-05-15T08:00:00Z",
                )
            repository.setAnimeList(listOf(animeTokyo, animeUtc))

            val sortedList = useCase(ListFilterCategory.ALL, SortOption.UPDATED_AT_DESC).first()
            assertEquals("UTC Anime", sortedList[0].displayTitle)
            assertEquals("Tokyo Anime", sortedList[1].displayTitle)
        }

    private fun createAnime(
        id: Long,
        title: String,
        status: UserAnimeStatus,
        score: Int,
        updatedAt: String = "2023-01-01",
    ) = UserAnime(
        id = id,
        originalTitle = title,
        englishTitle = title,
        displayTitle = title,
        subtitleTitle = null,
        imageUrl = null,
        mediaType = "TV",
        airingStatus = AiringStatus.FINISHED_AIRING,
        releaseSeason = "2020 Fall",
        totalEpisodes = 12,
        userStatus = status,
        userScore = score,
        watchedEpisodes = 12,
        isRewatching = false,
        updatedAt = updatedAt,
    )
}
