package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.model.IgnoredAnime
import com.gokova.myanimelist.core.domain.model.ImportIgnoredAnimeResult
import com.gokova.myanimelist.core.domain.repository.IgnoredAnimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class ExportIgnoredAnimeUseCaseTest {
    @Test
    fun `export writes csv header and records to output stream`() =
        runBlocking {
            val fakeRepo =
                FakeRepository(
                    listOf(
                        IgnoredAnime(
                            animeId = 1L,
                            title = "Anime One, With Comma",
                            titleEnglish = "Anime 1",
                            pictureUrl = "https://example.com/pic1.jpg",
                            createdAt = 1000L,
                        ),
                        IgnoredAnime(
                            animeId = 2L,
                            title = "Anime Two",
                            createdAt = 2000L,
                        ),
                    ),
                )

            val useCase = ExportIgnoredAnimeUseCase(fakeRepo)
            val outputStream = ByteArrayOutputStream()

            val result = useCase(outputStream)

            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrNull())

            val csvContent = outputStream.toString(Charsets.UTF_8.name())
            assertTrue(csvContent.startsWith("anime_id,title,title_english,picture_url,created_at"))
            assertTrue(csvContent.contains("\"Anime One, With Comma\""))
            assertTrue(csvContent.contains("Anime Two"))
        }

    @Test
    fun `export writes header and returns 0 when no ignored anime`() =
        runBlocking {
            val fakeRepo = FakeRepository(emptyList())
            val useCase = ExportIgnoredAnimeUseCase(fakeRepo)
            val outputStream = ByteArrayOutputStream()

            val result = useCase(outputStream)

            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull())

            val csvContent = outputStream.toString(Charsets.UTF_8.name())
            assertEquals("anime_id,title,title_english,picture_url,created_at\r\n", csvContent)
        }

    private class FakeRepository(
        private val list: List<IgnoredAnime>,
    ) : IgnoredAnimeRepository {
        override fun observeIgnoredAnimeIds(): Flow<List<Long>> =
            MutableStateFlow(list.map { it.animeId })

        override suspend fun getIgnoredAnimeIds(): List<Long> = list.map { it.animeId }

        override fun observeIsIgnored(animeId: Long): Flow<Boolean> =
            MutableStateFlow(list.any { it.animeId == animeId })

        override suspend fun isIgnored(animeId: Long): Boolean = list.any { it.animeId == animeId }

        override suspend fun setAnimeNotInterested(
            animeId: Long,
            title: String,
            titleEnglish: String?,
            pictureUrl: String?,
        ) {
            // Fake implementation
        }

        override suspend fun removeAnimeFromNotInterested(animeId: Long) {
            // Fake implementation
        }

        override fun observeAllIgnoredAnime(): Flow<List<IgnoredAnime>> = MutableStateFlow(list)

        override suspend fun getAllIgnoredAnime(): List<IgnoredAnime> = list

        override fun observeIgnoredAnimeCount(): Flow<Int> = MutableStateFlow(list.size)

        override suspend fun upsertIgnoredAnimeList(
            items: List<IgnoredAnime>,
        ): ImportIgnoredAnimeResult = ImportIgnoredAnimeResult.Success(items.size, items.size, 0)
    }
}
