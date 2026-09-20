package com.gokova.myanimelist.feature.recommendation.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SeasonalPeriodCalculatorTest {
    private val calculator = SeasonalPeriodCalculator()

    @Test
    fun `winter month produces correct current, next, and past seasons with year rollback`() {
        val cal =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, Calendar.FEBRUARY)
                set(Calendar.DAY_OF_MONTH, 15)
            }

        val result = calculator.getTargetSeasons(cal)

        assertEquals(4, result.size)
        assertEquals(SeasonPeriod(2026, "winter"), result[0])
        assertEquals(SeasonPeriod(2026, "spring"), result[1])
        assertEquals(SeasonPeriod(2025, "fall"), result[2])
        assertEquals(SeasonPeriod(2025, "summer"), result[3])
    }

    @Test
    fun `fall month produces correct current, next, and past seasons with year advance`() {
        val cal =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, Calendar.NOVEMBER)
                set(Calendar.DAY_OF_MONTH, 10)
            }

        val result = calculator.getTargetSeasons(cal)

        assertEquals(4, result.size)
        assertEquals(SeasonPeriod(2026, "fall"), result[0])
        assertEquals(SeasonPeriod(2027, "winter"), result[1])
        assertEquals(SeasonPeriod(2026, "summer"), result[2])
        assertEquals(SeasonPeriod(2026, "spring"), result[3])
    }

    @Test
    fun `summer month produces all four seasons in order`() {
        val cal =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, Calendar.AUGUST)
                set(Calendar.DAY_OF_MONTH, 1)
            }

        val result = calculator.getTargetSeasons(cal)

        assertEquals(4, result.size)
        assertEquals(SeasonPeriod(2026, "summer"), result[0])
        assertEquals(SeasonPeriod(2026, "fall"), result[1])
        assertEquals(SeasonPeriod(2026, "spring"), result[2])
        assertEquals(SeasonPeriod(2026, "winter"), result[3])
    }
}
