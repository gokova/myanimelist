package com.gokova.myanimelist.core.database.converter

import com.gokova.myanimelist.core.database.entity.GenreEntity
import com.gokova.myanimelist.core.database.entity.StudioEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AnimeTypeConvertersTest {
    private lateinit var converters: AnimeTypeConverters

    @Before
    fun setUp() {
        converters = AnimeTypeConverters()
    }

    @Test
    fun `fromGenreList and toGenreList round trips genre list correctly`() {
        val genres =
            listOf(
                GenreEntity(id = 1, name = "Action"),
                GenreEntity(id = 2, name = "Adventure"),
                GenreEntity(id = 58, name = "Gore"),
                GenreEntity(id = 27, name = "Shounen"),
            )

        val json = converters.fromGenreList(genres)
        val result = converters.toGenreList(json)

        assertEquals(genres, result)
    }

    @Test
    fun `fromStudioList and toStudioList round trips studio list correctly`() {
        val studios =
            listOf(
                StudioEntity(id = 858, name = "Wit Studio"),
                StudioEntity(id = 569, name = "MAPPA"),
                StudioEntity(id = 11, name = "Kyoto Animation"),
            )

        val json = converters.fromStudioList(studios)
        val result = converters.toStudioList(json)

        assertEquals(studios, result)
    }

    @Test
    fun `fromGenreList and toGenreList handle null and empty lists`() {
        assertNull(converters.fromGenreList(null))
        assertNull(converters.toGenreList(null))
        assertEquals(emptyList<GenreEntity>(), converters.toGenreList("[]"))
        assertEquals("[]", converters.fromGenreList(emptyList()))
    }

    @Test
    fun `fromStudioList and toStudioList handle null and empty lists`() {
        assertNull(converters.fromStudioList(null))
        assertNull(converters.toStudioList(null))
        assertEquals(emptyList<StudioEntity>(), converters.toStudioList("[]"))
        assertEquals("[]", converters.fromStudioList(emptyList()))
    }

    @Test
    fun `toGenreList and toStudioList ignore unknown keys in JSON`() {
        val genreJsonWithExtra =
            """[{"id":1,"name":"Action","description":"High intensity","extra":true}]"""
        val studioJsonWithExtra =
            """[{"id":43,"name":"Production I.G","established":1987}]"""

        val parsedGenres = converters.toGenreList(genreJsonWithExtra)
        val parsedStudios = converters.toStudioList(studioJsonWithExtra)

        assertEquals(listOf(GenreEntity(id = 1, name = "Action")), parsedGenres)
        assertEquals(listOf(StudioEntity(id = 43, name = "Production I.G")), parsedStudios)
    }
}
