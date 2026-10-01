package com.gokova.myanimelist.feature.profile.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ProfileDateFormatterTest {
    @Test
    fun `formats ISO timestamp with day-month-year for Germany locale`() {
        val result =
            ProfileDateFormatter.formatJoinedDate(
                rawDate = "2021-06-26T21:01:27+00:00",
                locale = Locale.GERMANY,
            )
        assertEquals("26.06.2021", result)
    }

    @Test
    fun `formats ISO timestamp with month-day-year for US locale`() {
        val result =
            ProfileDateFormatter.formatJoinedDate(
                rawDate = "2021-06-26T21:01:27+00:00",
                locale = Locale.US,
            )
        assertEquals("06.26.2021", result)
    }

    @Test
    fun `formats ISO timestamp with day-month-year for UK locale`() {
        val result =
            ProfileDateFormatter.formatJoinedDate(
                rawDate = "2021-06-26T21:01:27+00:00",
                locale = Locale.UK,
            )
        assertEquals("26.06.2021", result)
    }

    @Test
    fun `formats ISO timestamp with year-month-day for Japan locale`() {
        val result =
            ProfileDateFormatter.formatJoinedDate(
                rawDate = "2021-06-26T21:01:27+00:00",
                locale = Locale.JAPAN,
            )
        assertEquals("2021.06.26", result)
    }

    @Test
    fun `formats date-only string properly`() {
        val resultGerman =
            ProfileDateFormatter.formatJoinedDate(
                rawDate = "2021-06-26",
                locale = Locale.GERMANY,
            )
        assertEquals("26.06.2021", resultGerman)

        val resultUs =
            ProfileDateFormatter.formatJoinedDate(
                rawDate = "2021-06-26",
                locale = Locale.US,
            )
        assertEquals("06.26.2021", resultUs)
    }

    @Test
    fun `formats UTC Z timestamp properly`() {
        val result =
            ProfileDateFormatter.formatJoinedDate(
                rawDate = "2020-01-05T00:00:00Z",
                locale = Locale.GERMANY,
            )
        assertEquals("05.01.2020", result)
    }

    @Test
    fun `handles null and blank inputs gracefully`() {
        assertEquals("", ProfileDateFormatter.formatJoinedDate(null))
        assertEquals("", ProfileDateFormatter.formatJoinedDate(""))
        assertEquals("", ProfileDateFormatter.formatJoinedDate("   "))
    }

    @Test
    fun `handles malformed input without throwing exception`() {
        val invalid = "not-a-valid-date"
        val result = ProfileDateFormatter.formatJoinedDate(invalid, Locale.GERMANY)
        assertEquals(invalid, result)
    }
}
