package com.gokova.myanimelist.core.domain.taxonomy

val GENRE_NAMES: Set<String> =
    setOf(
        "Action",
        "Adventure",
        "Avant Garde",
        "Award Winning",
        "Comedy",
        "Drama",
        "Ecchi",
        "Fantasy",
        "Horror",
        "Mystery",
        "Romance",
        "Sci-Fi",
        "Slice of Life",
        "Supernatural",
        "Suspense",
    )

val THEME_NAMES: Set<String> =
    setOf(
        // Demographics
        "Kids",
        "Seinen",
        "Shoujo",
        "Shounen",
        // Themes & Tropes
        "Adult Cast",
        "Anthropomorphic",
        "Childcare",
        "Detective",
        "Educational",
        "Gore",
        "Historical",
        "Isekai",
        "Iyashikei",
        "Mahou Shoujo",
        "Martial Arts",
        "Mecha",
        "Medical",
        "Military",
        "Mythology",
        "Organized Crime",
        "Psychological",
        "Reincarnation",
        "School",
        "Space",
        "Strategy Game",
        "Super Power",
        "Survival",
        "Time Travel",
        "Urban Fantasy",
        "Video Game",
        "Workplace",
    )

enum class TagType {
    GENRE,
    THEME,
}

fun isGenre(name: String): Boolean = GENRE_NAMES.contains(name)

fun isTheme(name: String): Boolean = THEME_NAMES.contains(name)

fun classifyTag(name: String): TagType =
    when {
        GENRE_NAMES.contains(name) -> TagType.GENRE
        THEME_NAMES.contains(name) -> TagType.THEME
        else -> {
            // TODO: Log unclassified genre/theme tag to Crashlytics/Timber when logging library is added
            TagType.THEME
        }
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
