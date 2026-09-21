package com.gokova.myanimelist.core.domain.taxonomy

import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.domain.logging.Logger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnimeTaxonomyTest {
    @Before
    fun setUp() {
        clearReportedUnknownTags()
    }

    @Test
    fun classifyTag_whenGenre_returnsGenre() {
        assertEquals(TagType.GENRE, classifyTag("Action"))
        assertEquals(TagType.GENRE, classifyTag("Fantasy"))
        assertEquals(TagType.GENRE, classifyTag("Romance"))
        assertEquals(TagType.GENRE, classifyTag("Sports"))
        assertEquals(TagType.GENRE, classifyTag("Gourmet"))
        assertEquals(TagType.GENRE, classifyTag("Boys Love"))
        assertTrue(isGenre("Action"))
        assertTrue(isGenre("Sports"))
    }

    @Test
    fun classifyTag_whenTheme_returnsTheme() {
        assertEquals(TagType.THEME, classifyTag("Isekai"))
        assertEquals(TagType.THEME, classifyTag("School"))
        assertEquals(TagType.THEME, classifyTag("Shounen"))
        assertEquals(TagType.THEME, classifyTag("Josei"))
        assertEquals(TagType.THEME, classifyTag("Racing"))
        assertEquals(TagType.THEME, classifyTag("Samurai"))
        assertEquals(TagType.THEME, classifyTag("Music"))
        assertTrue(isTheme("Isekai"))
        assertTrue(isTheme("Racing"))
    }

    @Test
    fun classifyTag_whenUnknown_defaultsToTheme() {
        assertEquals(TagType.THEME, classifyTag("NewUncatalogedGenre"))
    }

    @Test
    fun classifyTag_whenUnknown_logsWarningWithExceptionAndDeduplicates() {
        val loggedWarnings = mutableListOf<Pair<Throwable?, String>>()
        val testLogger =
            TestLogger { throwable, message ->
                loggedWarnings.add(throwable to message)
            }
        AppLog.init { testLogger }

        classifyTag("BrandNewGenre")
        assertEquals(1, loggedWarnings.size)
        val (firstThrowable, firstMsg) = loggedWarnings.first()
        assertTrue(firstThrowable is UnclassifiedTaxonomyTagException)
        assertEquals("BrandNewGenre", (firstThrowable as UnclassifiedTaxonomyTagException).tagName)
        assertTrue(firstMsg.contains("BrandNewGenre"))

        // Duplicate call with the same tag should be deduplicated
        classifyTag("BrandNewGenre")
        assertEquals(1, loggedWarnings.size)

        // New unknown tag should be logged
        classifyTag("AnotherNewGenre")
        assertEquals(2, loggedWarnings.size)
    }

    @Test
    fun partitionGenresAndThemes_separatesCorrectly() {
        val tags = listOf("Action", "Isekai", "Comedy", "School", "UnknownTag")
        val (genres, themes) = tags.partitionGenresAndThemes { it }

        assertEquals(listOf("Action", "Comedy"), genres)
        assertEquals(listOf("Isekai", "School", "UnknownTag"), themes)
    }
}

private class TestLogger(
    private val onWarning: (Throwable?, String) -> Unit,
) : Logger {
    override fun v(message: () -> String) = Unit

    override fun d(message: () -> String) = Unit

    override fun i(message: () -> String) = Unit

    override fun w(
        throwable: Throwable?,
        message: () -> String,
    ) {
        onWarning(throwable, message())
    }

    override fun e(
        throwable: Throwable?,
        message: () -> String,
    ) = Unit
}
