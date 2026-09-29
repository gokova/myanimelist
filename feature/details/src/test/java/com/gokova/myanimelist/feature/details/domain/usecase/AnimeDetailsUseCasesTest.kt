package com.gokova.myanimelist.feature.details.domain.usecase

import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.fakes.FakeAnimeDetailsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnimeDetailsUseCasesTest {
    private lateinit var repository: FakeAnimeDetailsRepository
    private lateinit var observeUseCase: ObserveAnimeDetailsUseCase
    private lateinit var refreshUseCase: RefreshAnimeDetailsUseCase
    private lateinit var addUseCase: AddAnimeToMyListUseCase

    private val testDetails =
        AnimeDetails(
            id = 42L,
            displayTitle = "Test Anime",
            subtitleTitle = null,
            originalTitle = "Test Anime",
            englishTitle = "Test Anime",
            mainPictureMedium = null,
            mainPictureLarge = null,
            synopsis = "A test synopsis",
            genres = listOf("Action"),
            themes = listOf("School"),
            meanScore = 8.0,
            userScore = null,
            userStatus = null,
            isInUserList = false,
            rank = 10,
            popularity = 5,
            numListUsers = 1000,
            numScoringUsers = 500,
            mediaType = "tv",
            status = "finished_airing",
            season = "2023 Spring",
            numEpisodes = 12,
            durationSeconds = 1440,
        )

    @Before
    fun setUp() {
        repository = FakeAnimeDetailsRepository()
        observeUseCase = ObserveAnimeDetailsUseCase(repository)
        refreshUseCase = RefreshAnimeDetailsUseCase(repository)
        addUseCase = AddAnimeToMyListUseCase(repository)
    }

    @Test
    fun `observeAnimeDetails emits repository flow`() =
        runTest {
            repository.emitDetails(testDetails)

            val result = observeUseCase(42L).first()

            assertEquals(testDetails, result)
        }

    @Test
    fun `refreshAnimeDetails returns success result`() =
        runTest {
            repository.fetchResult = Result.success(testDetails)

            val result = refreshUseCase(42L)

            assertTrue(result.isSuccess)
            assertEquals(testDetails, result.getOrNull())
        }

    @Test
    fun `refreshAnimeDetails returns failure on repository error`() =
        runTest {
            val error = RuntimeException("Network down")
            repository.fetchResult = Result.failure(error)

            val result = refreshUseCase(42L)

            assertTrue(result.isFailure)
            assertEquals(error, result.exceptionOrNull())
        }

    @Test
    fun `addAnimeToMyList invokes repository with animeId`() =
        runTest {
            val result = addUseCase(42L)

            assertTrue(result.isSuccess)
            assertEquals(42L, repository.addCalledWithId)
        }
}
