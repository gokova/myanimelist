package com.gokova.myanimelist.core.domain.taxonomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimeTaxonomyTest {
    @Test
    fun classifyTag_whenGenre_returnsGenre() {
        assertEquals(TagType.GENRE, classifyTag("Action"))
        assertEquals(TagType.GENRE, classifyTag("Fantasy"))
        assertEquals(TagType.GENRE, classifyTag("Romance"))
        assertTrue(isGenre("Action"))
    }

    @Test
    fun classifyTag_whenTheme_returnsTheme() {
        assertEquals(TagType.THEME, classifyTag("Isekai"))
        assertEquals(TagType.THEME, classifyTag("School"))
        assertEquals(TagType.THEME, classifyTag("Shounen"))
        assertTrue(isTheme("Isekai"))
    }

    @Test
    fun classifyTag_whenUnknown_defaultsToTheme() {
        assertEquals(TagType.THEME, classifyTag("NewUncatalogedGenre"))
    }

    @Test
    fun partitionGenresAndThemes_separatesCorrectly() {
        val tags = listOf("Action", "Isekai", "Comedy", "School", "UnknownTag")
        val (genres, themes) = tags.partitionGenresAndThemes { it }

        assertEquals(listOf("Action", "Comedy"), genres)
        assertEquals(listOf("Isekai", "School", "UnknownTag"), themes)
    }
}
