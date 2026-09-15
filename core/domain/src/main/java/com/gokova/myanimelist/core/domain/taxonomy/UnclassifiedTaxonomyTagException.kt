package com.gokova.myanimelist.core.domain.taxonomy

/**
 * Thrown or logged when an anime taxonomy tag is unclassified in [GENRE_NAMES] and [THEME_NAMES].
 */
class UnclassifiedTaxonomyTagException(
    val tagName: String,
) : Exception("Unclassified anime taxonomy tag: '$tagName'")
