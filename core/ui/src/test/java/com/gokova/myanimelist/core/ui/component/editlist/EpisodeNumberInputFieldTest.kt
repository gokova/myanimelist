package com.gokova.myanimelist.core.ui.component.editlist

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeNumberInputFieldTest {
    @Test
    fun `sanitizeEpisodeInput with empty text returns empty text and null parsed`() {
        val input = TextFieldValue("")
        val (sanitized, parsed) = sanitizeEpisodeInput(newValue = input, totalEpisodes = 24)

        assertEquals("", sanitized.text)
        assertNull(parsed)
    }

    @Test
    fun `sanitizeEpisodeInput with non-digits returns empty text and null parsed`() {
        val input = TextFieldValue("abc")
        val (sanitized, parsed) = sanitizeEpisodeInput(newValue = input, totalEpisodes = 24)

        assertEquals("", sanitized.text)
        assertNull(parsed)
    }

    @Test
    fun `sanitizeEpisodeInput with valid number within totalEpisodes returns number`() {
        val input = TextFieldValue("12", selection = TextRange(2))
        val (sanitized, parsed) = sanitizeEpisodeInput(newValue = input, totalEpisodes = 24)

        assertEquals("12", sanitized.text)
        assertEquals(12, parsed)
        assertEquals(TextRange(2), sanitized.selection)
    }

    @Test
    fun `sanitizeEpisodeInput with number exceeding totalEpisodes clamps to totalEpisodes`() {
        val input = TextFieldValue("25", selection = TextRange(2))
        val (sanitized, parsed) = sanitizeEpisodeInput(newValue = input, totalEpisodes = 24)

        assertEquals("24", sanitized.text)
        assertEquals(24, parsed)
        assertEquals(TextRange(2), sanitized.selection)
    }

    @Test
    fun `sanitizeEpisodeInput with oversized number clamps to known total`() {
        val input = TextFieldValue("99999999999999999999", selection = TextRange(20))
        val (sanitized, parsed) = sanitizeEpisodeInput(newValue = input, totalEpisodes = 24)

        assertEquals("24", sanitized.text)
        assertEquals(24, parsed)
        assertEquals(TextRange(2), sanitized.selection)
    }

    @Test
    fun `sanitizeEpisodeInput with oversized number and null total rejects and keeps current`() {
        val current = TextFieldValue("1050", selection = TextRange(4))
        val input = TextFieldValue("99999999999999999999", selection = TextRange(20))
        val (sanitized, parsed) =
            sanitizeEpisodeInput(
                newValue = input,
                totalEpisodes = null,
                currentValue = current,
            )

        assertEquals("1050", sanitized.text)
        assertNull(parsed)
        assertEquals(TextRange(4), sanitized.selection)
    }

    @Test
    fun `sanitizeEpisodeInput with oversized number and zero total rejects and keeps current`() {
        val current = TextFieldValue("500", selection = TextRange(3))
        val input = TextFieldValue("99999999999999999999", selection = TextRange(20))
        val (sanitized, parsed) =
            sanitizeEpisodeInput(
                newValue = input,
                totalEpisodes = 0,
                currentValue = current,
            )

        assertEquals("500", sanitized.text)
        assertNull(parsed)
        assertEquals(TextRange(3), sanitized.selection)
    }

    @Test
    fun `sanitizeEpisodeInput with leading zeros normalizes to integer value`() {
        val input = TextFieldValue("007", selection = TextRange(3))
        val (sanitized, parsed) = sanitizeEpisodeInput(newValue = input, totalEpisodes = 24)

        assertEquals("7", sanitized.text)
        assertEquals(7, parsed)
        assertEquals(TextRange(1), sanitized.selection)
    }

    @Test
    fun `sanitizeEpisodeInput with multiple zeros parses as 0`() {
        val input = TextFieldValue("000", selection = TextRange(3))
        val (sanitized, parsed) = sanitizeEpisodeInput(newValue = input, totalEpisodes = 24)

        assertEquals("0", sanitized.text)
        assertEquals(0, parsed)
        assertEquals(TextRange(1), sanitized.selection)
    }

    @Test
    fun `sanitizeEpisodeInput without totalEpisodes allows valid large Int number`() {
        val input = TextFieldValue("1050", selection = TextRange(4))
        val (sanitized, parsed) = sanitizeEpisodeInput(newValue = input, totalEpisodes = null)

        assertEquals("1050", sanitized.text)
        assertEquals(1050, parsed)
        assertEquals(TextRange(4), sanitized.selection)
    }
}
