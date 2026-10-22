package com.gokova.myanimelist.core.database.repository

import com.gokova.myanimelist.core.database.dao.IgnoredAnimeDao
import com.gokova.myanimelist.core.database.entity.IgnoredAnimeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IgnoredAnimeRepositoryImplTest {
    private lateinit var fakeDao: FakeIgnoredAnimeDao
    private lateinit var repository: IgnoredAnimeRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeIgnoredAnimeDao()
        repository = IgnoredAnimeRepositoryImpl(fakeDao)
    }

    @Test
    fun `setAnimeNotInterested delegates to dao upsert`() =
        runTest {
            repository.setAnimeNotInterested(
                animeId = 123L,
                title = "Test Anime",
                titleEnglish = "Test Anime EN",
                pictureUrl = "https://example.com/pic.jpg",
            )

            assertTrue(repository.isIgnored(123L))
            val all = repository.observeAllIgnoredAnime().first()
            assertEquals(1, all.size)
            assertEquals(123L, all[0].animeId)
            assertEquals("Test Anime", all[0].title)
            assertEquals("Test Anime EN", all[0].titleEnglish)
            assertEquals("https://example.com/pic.jpg", all[0].pictureUrl)
        }

    @Test
    fun `removeAnimeFromNotInterested delegates to dao delete`() =
        runTest {
            repository.setAnimeNotInterested(123L, "Test Anime")
            assertTrue(repository.isIgnored(123L))

            repository.removeAnimeFromNotInterested(123L)
            assertFalse(repository.isIgnored(123L))
        }

    @Test
    fun `observeIgnoredAnimeIds emits current ids`() =
        runTest {
            repository.setAnimeNotInterested(1L, "Anime 1")
            repository.setAnimeNotInterested(2L, "Anime 2")

            val ids = repository.observeIgnoredAnimeIds().first()
            assertEquals(listOf(1L, 2L), ids)
        }

    private class FakeIgnoredAnimeDao : IgnoredAnimeDao {
        private val storage = MutableStateFlow<Map<Long, IgnoredAnimeEntity>>(emptyMap())

        override fun observeIgnoredAnimeIds(): Flow<List<Long>> = storage.map { it.keys.toList() }

        override suspend fun getIgnoredAnimeIds(): List<Long> = storage.value.keys.toList()

        override fun observeIsIgnored(animeId: Long): Flow<Boolean> =
            storage.map { it.containsKey(animeId) }

        override suspend fun isIgnored(animeId: Long): Boolean = storage.value.containsKey(animeId)

        override suspend fun upsertIgnoredAnime(entity: IgnoredAnimeEntity) {
            storage.value += (entity.animeId to entity)
        }

        override suspend fun deleteIgnoredAnime(animeId: Long): Int {
            val existed = storage.value.containsKey(animeId)
            storage.value -= animeId
            return if (existed) 1 else 0
        }

        override fun observeAllIgnoredAnime(): Flow<List<IgnoredAnimeEntity>> =
            storage.map { it.values.toList() }

        override suspend fun getAllIgnoredAnime(): List<IgnoredAnimeEntity> =
            storage.value.values.toList()
    }
}
