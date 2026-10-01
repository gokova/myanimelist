package com.gokova.myanimelist.feature.profile.presentation.util

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for formatting date strings (such as MAL profile `joined_at`)
 * into locale-sensitive dot-separated date notation (e.g. `26.06.2021` or `06.26.2021`).
 */
object ProfileDateFormatter {
    private const val DEFAULT_PATTERN = "dd.MM.yyyy"
    private val ISO_DATE_REGEX = Regex("""^(\d{4})-(\d{2})-(\d{2})""")

    private val PARSE_PATTERNS =
        arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
        )

    /**
     * Resolves a dot-separated date pattern (e.g. `dd.MM.yyyy`, `MM.dd.yyyy`, or `yyyy.MM.dd`)
     * derived from the date component ordering of the provided [locale].
     */
    fun getDotDatePattern(locale: Locale = Locale.getDefault()): String {
        val shortPattern =
            (DateFormat.getDateInstance(DateFormat.SHORT, locale) as? SimpleDateFormat)?.toPattern()

        val dIndex = shortPattern?.indexOfAny(charArrayOf('d', 'D')) ?: -1
        val mIndex = shortPattern?.indexOfAny(charArrayOf('M', 'L')) ?: -1
        val yIndex = shortPattern?.indexOfAny(charArrayOf('y', 'Y')) ?: -1

        return if (dIndex != -1 && mIndex != -1 && yIndex != -1) {
            val components =
                listOf(
                    dIndex to "dd",
                    mIndex to "MM",
                    yIndex to "yyyy",
                ).sortedBy { it.first }
            components.joinToString(separator = ".") { it.second }
        } else {
            DEFAULT_PATTERN
        }
    }

    /**
     * Formats an ISO-8601 date string (e.g. `2021-06-26T21:01:27+00:00` or `2021-06-26`)
     * into dot-separated notation based on the ordering of day, month, and year for the [locale].
     *
     * Example outputs:
     * - Germany / UK / Europe: `26.06.2021`
     * - US: `06.26.2021`
     * - Japan: `2021.06.26`
     */
    fun formatJoinedDate(
        rawDate: String?,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (rawDate.isNullOrBlank()) return ""

        val pattern = getDotDatePattern(locale)
        val match = ISO_DATE_REGEX.find(rawDate.trim())

        return if (match != null) {
            val (year, month, day) = match.destructured
            pattern
                .replace("yyyy", year)
                .replace("MM", month)
                .replace("dd", day)
        } else {
            formatViaPatterns(rawDate, pattern, locale)
        }
    }

    private fun formatViaPatterns(
        rawDate: String,
        pattern: String,
        locale: Locale,
    ): String {
        val parsedDate =
            PARSE_PATTERNS.firstNotNullOfOrNull { parsePattern ->
                try {
                    val parser =
                        SimpleDateFormat(parsePattern, Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                    parser.parse(rawDate)
                } catch (_: ParseException) {
                    null
                }
            } ?: return rawDate

        val formatter =
            SimpleDateFormat(pattern, locale).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        return formatter.format(parsedDate)
    }
}
