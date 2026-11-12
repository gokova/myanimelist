package com.gokova.myanimelist.feature.franchise.domain.model

data class FranchiseNode(
    val animeId: Long,
    val title: String,
    val titleEnglish: String?,
    val posterMedium: String?,
    val posterLarge: String?,
    val mediaType: String?,
    val numEpisodes: Int?,
    val isInUserList: Boolean,
    val userStatus: String?,
    val episodesWatched: Int,
    val isRewatching: Boolean,
    val isNotInterested: Boolean,
    val isOrigin: Boolean,
) {
    val displayTitle: String get() = titleEnglish?.takeIf { it.isNotBlank() } ?: title

    val formattedMediaType: String
        get() =
            when (mediaType?.lowercase()) {
                "tv" -> "TV"
                "movie" -> "Movie"
                "ova" -> "OVA"
                "ona" -> "ONA"
                "special" -> "Special"
                "tv_special" -> "TV Special"
                "music" -> "Music"
                "pv" -> "PV"
                "cm" -> "CM"
                null -> "Anime"
                else ->
                    mediaType
                        .split('_')
                        .joinToString(" ") { word ->
                            word.replaceFirstChar { it.uppercase() }
                        }
            }
}
