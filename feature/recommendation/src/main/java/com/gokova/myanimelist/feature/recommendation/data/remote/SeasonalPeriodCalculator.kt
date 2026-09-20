package com.gokova.myanimelist.feature.recommendation.data.remote

import java.util.Calendar
import java.util.TimeZone

data class SeasonPeriod(
    val year: Int,
    val season: String,
)

class SeasonalPeriodCalculator {
    fun getTargetSeasons(calendar: Calendar = defaultCalendar()): List<SeasonPeriod> {
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        val currentSeasonIdx =
            when (month) {
                Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH -> 0
                Calendar.APRIL, Calendar.MAY, Calendar.JUNE -> 1
                Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER -> 2
                else -> 3
            }

        val current = SeasonPeriod(year, SEASONS[currentSeasonIdx])
        val next = offsetSeason(year, currentSeasonIdx, 1)
        val last = offsetSeason(year, currentSeasonIdx, -1)
        val twoAgo = offsetSeason(year, currentSeasonIdx, -2)

        return listOf(current, next, last, twoAgo)
    }

    private fun offsetSeason(
        baseYear: Int,
        baseSeasonIdx: Int,
        offset: Int,
    ): SeasonPeriod {
        var newIdx = baseSeasonIdx + offset
        var newYear = baseYear
        while (newIdx < 0) {
            newIdx += SEASONS.size
            newYear -= 1
        }
        while (newIdx >= SEASONS.size) {
            newIdx -= SEASONS.size
            newYear += 1
        }
        return SeasonPeriod(newYear, SEASONS[newIdx])
    }

    private fun defaultCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    companion object {
        val SEASONS = listOf("winter", "spring", "summer", "fall")
    }
}
