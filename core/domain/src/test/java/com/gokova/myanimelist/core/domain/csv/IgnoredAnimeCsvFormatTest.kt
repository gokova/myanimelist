package com.gokova.myanimelist.core.domain.csv

import com.gokova.myanimelist.core.domain.model.IgnoredAnime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IgnoredAnimeCsvFormatTest {
    @Test
    fun `serialize and parse roundtrip simple records`() {
        val original =
            listOf(
                IgnoredAnime(
                    animeId = 1L,
                    title = "Sousou no Frieren",
                    titleEnglish = "Frieren: Beyond Journey's End",
                    pictureUrl = "https://cdn.myanimelist.net/images/anime/1015/138006.jpg",
                    createdAt = 1700000000000L,
                ),
                IgnoredAnime(
                    animeId = 2L,
                    title = "Steins;Gate",
                    titleEnglish = "Steins;Gate",
                    pictureUrl = "https://cdn.myanimelist.net/images/anime/7/3791.jpg",
                    createdAt = 1700000001000L,
                ),
            )

        val csv = IgnoredAnimeCsvFormat.serialize(original)
        val parsed = IgnoredAnimeCsvFormat.parse(csv)

        assertEquals(original.size, parsed.size)
        assertEquals(original[0], parsed[0])
        assertEquals(original[1], parsed[1])
    }

    @Test
    fun `serialize escapes titles with commas and quotes`() {
        val items =
            listOf(
                IgnoredAnime(
                    animeId = 100L,
                    title = "Bleach: Thousand-Year Blood War - The Conflict, Part 2",
                    titleEnglish = "Bleach: Part 2",
                    pictureUrl = null,
                    createdAt = 123456789L,
                ),
                IgnoredAnime(
                    animeId = 101L,
                    title = "Movie \"Special Edition\", Subtitle",
                    titleEnglish = null,
                    pictureUrl = null,
                    createdAt = 987654321L,
                ),
            )

        val csv = IgnoredAnimeCsvFormat.serialize(items)
        assertTrue(csv.contains("\"Bleach: Thousand-Year Blood War - The Conflict, Part 2\""))
        assertTrue(csv.contains("\"Movie \"\"Special Edition\"\", Subtitle\""))

        val parsed = IgnoredAnimeCsvFormat.parse(csv)
        assertEquals(2, parsed.size)
        assertEquals("Bleach: Thousand-Year Blood War - The Conflict, Part 2", parsed[0].title)
        assertEquals("Movie \"Special Edition\", Subtitle", parsed[1].title)
    }

    @Test
    fun `parse handles tolerant column mapping and missing optional columns`() {
        val customCsv =
            """
            Title, Anime_Id
            "Frieren, The Mage", 52991
            "Steins Gate", 9253
            """.trimIndent()

        val parsed = IgnoredAnimeCsvFormat.parse(customCsv)
        assertEquals(2, parsed.size)

        assertEquals(52991L, parsed[0].animeId)
        assertEquals("Frieren, The Mage", parsed[0].title)
        assertNull(parsed[0].titleEnglish)
        assertNull(parsed[0].pictureUrl)
        assertTrue(parsed[0].createdAt > 0)

        assertEquals(9253L, parsed[1].animeId)
        assertEquals("Steins Gate", parsed[1].title)
    }

    @Test
    fun `parse handles positional layout when header is omitted`() {
        val noHeaderCsv =
            """
            52991,"Frieren, The Beyond",Frieren,https://example.com/pic.jpg,1700000000000
            """.trimIndent()

        val parsed = IgnoredAnimeCsvFormat.parse(noHeaderCsv)
        assertEquals(1, parsed.size)
        assertEquals(52991L, parsed[0].animeId)
        assertEquals("Frieren, The Beyond", parsed[0].title)
        assertEquals("Frieren", parsed[0].titleEnglish)
        assertEquals("https://example.com/pic.jpg", parsed[0].pictureUrl)
        assertEquals(1700000000000L, parsed[0].createdAt)
    }

    @Test
    fun `parse throws on unclosed quote`() {
        val brokenCsv =
            """
            anime_id,title
            1,"Broken title without closing quote
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            IgnoredAnimeCsvFormat.parse(brokenCsv)
        }
    }

    @Test
    fun `parse returns empty list on empty or blank string`() {
        assertTrue(IgnoredAnimeCsvFormat.parse("").isEmpty())
        assertTrue(IgnoredAnimeCsvFormat.parse("   \r\n   ").isEmpty())
    }

    @Test
    fun `parse strips leading UTF-8 BOM from header`() {
        val bomCsv = "\uFEFFanime_id,title\r\n100,Bleach\r\n"
        val parsed = IgnoredAnimeCsvFormat.parse(bomCsv)
        assertEquals(1, parsed.size)
        assertEquals(100L, parsed[0].animeId)
        assertEquals("Bleach", parsed[0].title)
    }

    @Test
    fun `parse skips malformed rows while parsing valid rows`() {
        val mixedCsv =
            """
            anime_id,title
            1,"Valid Row 1"
            2,Invalid"Quote In Unquoted
            3,"Valid Row 2"
            4,"Chars After Quote"extra
            5,"Valid Row 3"
            """.trimIndent()

        val parsed = IgnoredAnimeCsvFormat.parse(mixedCsv)
        assertEquals(3, parsed.size)
        assertEquals(1L, parsed[0].animeId)
        assertEquals("Valid Row 1", parsed[0].title)
        assertEquals(3L, parsed[1].animeId)
        assertEquals("Valid Row 2", parsed[1].title)
        assertEquals(5L, parsed[2].animeId)
        assertEquals("Valid Row 3", parsed[2].title)
    }

    @Test
    fun `parse throws IllegalArgumentException when all records are malformed`() {
        val allBadCsv =
            """
            anime_id,title
            1,Invalid"Quotes
            2,"Invalid"Extra
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            IgnoredAnimeCsvFormat.parse(allBadCsv)
        }
    }
}
