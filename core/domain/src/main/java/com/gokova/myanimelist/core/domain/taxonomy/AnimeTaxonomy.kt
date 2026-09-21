package com.gokova.myanimelist.core.domain.taxonomy

import com.gokova.myanimelist.core.domain.logging.AppLog
import java.util.concurrent.ConcurrentHashMap

val GENRE_NAMES: Set<String> =
    setOf(
        "Action",
        "Adventure",
        "Avant Garde",
        "Award Winning",
        "Boys Love",
        "Comedy",
        "Drama",
        "Ecchi",
        "Erotica",
        "Fantasy",
        "Girls Love",
        "Gourmet",
        "Hentai",
        "Horror",
        "Mystery",
        "Romance",
        "Sci-Fi",
        "Slice of Life",
        "Sports",
        "Supernatural",
        "Suspense",
    )

val THEME_NAMES: Set<String> =
    setOf(
        // Demographics
        "Josei",
        "Kids",
        "Seinen",
        "Shoujo",
        "Shounen",
        // Themes & Tropes
        "Adult Cast",
        "Anthropomorphic",
        "CGDCT",
        "Childcare",
        "Combat Sports",
        "Crossdressing",
        "Delinquents",
        "Detective",
        "Educational",
        "Gag Humor",
        "Gore",
        "Harem",
        "High Stakes Game",
        "Historical",
        "Idols (Female)",
        "Idols (Male)",
        "Isekai",
        "Iyashikei",
        "Love Polygon",
        "Love Status Quo",
        "Magical Sex Shift",
        "Mahou Shoujo",
        "Martial Arts",
        "Mecha",
        "Medical",
        "Military",
        "Music",
        "Mythology",
        "Organized Crime",
        "Otaku Culture",
        "Parody",
        "Performing Arts",
        "Psychological",
        "Racing",
        "Reincarnation",
        "Reverse Harem",
        "Samurai",
        "School",
        "Showbiz",
        "Space",
        "Strategy Game",
        "Super Power",
        "Survival",
        "Team Sports",
        "Time Travel",
        "Urban Fantasy",
        "Vampire",
        "Video Game",
        "Villainess",
        "Visual Arts",
        "Workplace",
    )

enum class TagType {
    GENRE,
    THEME,
}

fun isGenre(name: String): Boolean = GENRE_NAMES.contains(name)

fun isTheme(name: String): Boolean = THEME_NAMES.contains(name)

private val reportedUnknownTags: MutableSet<String> = ConcurrentHashMap.newKeySet()

fun classifyTag(name: String): TagType =
    when {
        GENRE_NAMES.contains(name) -> TagType.GENRE
        THEME_NAMES.contains(name) -> TagType.THEME
        else -> {
            if (reportedUnknownTags.add(name)) {
                AppLog.domain.w(UnclassifiedTaxonomyTagException(name)) {
                    "Unclassified genre/theme tag: '$name' (falling back to THEME)"
                }
            }
            TagType.THEME
        }
    }

internal fun clearReportedUnknownTags() {
    reportedUnknownTags.clear()
}

fun <T> Iterable<T>.partitionGenresAndThemes(nameSelector: (T) -> String): Pair<List<T>, List<T>> {
    val genres = mutableListOf<T>()
    val themes = mutableListOf<T>()
    for (item in this) {
        when (classifyTag(nameSelector(item))) {
            TagType.GENRE -> genres.add(item)
            TagType.THEME -> themes.add(item)
        }
    }
    return Pair(genres, themes)
}
