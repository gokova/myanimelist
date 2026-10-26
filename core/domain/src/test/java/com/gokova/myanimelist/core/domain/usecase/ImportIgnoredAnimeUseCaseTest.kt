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
import java.io.ByteArrayInputStream

class ImportIgnoredAnimeUseCaseTest {
    @Test
    fun `import parses csv and delegates to repository upsert`() =
        runBlocking {
            val fakeRepo = FakeRepository()
            val useCase = ImportIgnoredAnimeUseCase(fakeRepo)

            val csv =
                """
                anime_id,title,title_english,picture_url,created_at
                1,"Frieren, The Beyond",Frieren,https://example.com/pic.jpg,1000
                2,"Steins Gate",,https://example.com/sg.jpg,2000
                """.trimIndent()

            val inputStream = ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))
            val result = useCase(inputStream)

            assertTrue(result is ImportIgnoredAnimeResult.Success)
            val success = result as ImportIgnoredAnimeResult.Success
            assertEquals(2, success.totalCount)
            assertEquals(2, fakeRepo.insertedItems.size)
            assertEquals("Frieren, The Beyond", fakeRepo.insertedItems[0].title)
        }

    @Test
    fun `import returns EmptyFile for empty input`() =
        runBlocking {
            val fakeRepo = FakeRepository()
            val useCase = ImportIgnoredAnimeUseCase(fakeRepo)

            val inputStream = ByteArrayInputStream("".toByteArray(Charsets.UTF_8))
            val result = useCase(inputStream)

            assertTrue(result is ImportIgnoredAnimeResult.Failure.EmptyFile)
        }

    @Test
    fun `import returns InvalidFormat for malformed csv`() =
        runBlocking {
            val fakeRepo = FakeRepository()
            val useCase = ImportIgnoredAnimeUseCase(fakeRepo)

            val brokenCsv =
                """
                anime_id,title
                1,"Unclosed quote
                """.trimIndent()

            val inputStream = ByteArrayInputStream(brokenCsv.toByteArray(Charsets.UTF_8))
            val result = useCase(inputStream)

            assertTrue(result is ImportIgnoredAnimeResult.Failure.InvalidFormat)
        }

    @Test
    fun `import skips malformed lines and imports remaining valid records`() =
        runBlocking {
            val fakeRepo = FakeRepository()
            val useCase = ImportIgnoredAnimeUseCase(fakeRepo)

            val mixedCsv =
                """
                anime_id,title
                1,"Valid Record 1"
                2,Bad"Quote
                3,"Valid Record 2"
                """.trimIndent()

            val inputStream = ByteArrayInputStream(mixedCsv.toByteArray(Charsets.UTF_8))
            val result = useCase(inputStream)

            assertTrue(result is ImportIgnoredAnimeResult.Success)
            val success = result as ImportIgnoredAnimeResult.Success
            assertEquals(2, success.totalCount)
            assertEquals(2, fakeRepo.insertedItems.size)
            assertEquals(1L, fakeRepo.insertedItems[0].animeId)
            assertEquals(3L, fakeRepo.insertedItems[1].animeId)
        }

    private class FakeRepository : IgnoredAnimeRepository {
        val insertedItems = mutableListOf<IgnoredAnime>()

        override fun observeIgnoredAnimeIds(): Flow<List<Long>> = MutableStateFlow(emptyList())

        override suspend fun getIgnoredAnimeIds(): List<Long> = emptyList()

        override fun observeIsIgnored(animeId: Long): Flow<Boolean> = MutableStateFlow(false)

        override suspend fun isIgnored(animeId: Long): Boolean = false

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

        override fun observeAllIgnoredAnime(): Flow<List<IgnoredAnime>> =
            MutableStateFlow(emptyList())

        override suspend fun getAllIgnoredAnime(): List<IgnoredAnime> = emptyList()

        override fun observeIgnoredAnimeCount(): Flow<Int> = MutableStateFlow(0)

        override suspend fun upsertIgnoredAnimeList(
            items: List<IgnoredAnime>,
        ): ImportIgnoredAnimeResult {
            insertedItems.addAll(items)
            return ImportIgnoredAnimeResult.Success(
                totalCount = items.size,
                newCount = items.size,
                updatedCount = 0,
            )
        }
    }
}
