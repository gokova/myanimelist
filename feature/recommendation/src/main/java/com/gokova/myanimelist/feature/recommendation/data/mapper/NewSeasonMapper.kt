package com.gokova.myanimelist.feature.recommendation.data.mapper

import com.gokova.myanimelist.core.database.model.NewSeasonAnimeItem
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime
import java.util.Locale

object NewSeasonMapper {
    fun toDomain(item: NewSeasonAnimeItem): NewSeasonAnime {
        val anime = item.anime
        val parent = item.parentAnime
        val relation = item.newSeason

        val englishTitle = anime.titleEnglish?.takeIf { it.isNotBlank() }
        val displayTitle = englishTitle ?: anime.title
        val subtitleTitle =
            if (englishTitle != null && !englishTitle.equals(anime.title, ignoreCase = true)) {
                anime.title
            } else {
                null
            }

        val parentEnglish = parent.titleEnglish?.takeIf { it.isNotBlank() }
        val parentTitle = parentEnglish ?: parent.title

        val year = anime.startSeasonYear
        val season = anime.startSeasonSeason
        val releaseSeason =
            if (year != null && season != null) {
                val capitalizedSeason =
                    season.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
                    }
                "$year $capitalizedSeason"
            } else if (year != null) {
                "$year"
            } else {
                null
            }

        return NewSeasonAnime(
            animeId = anime.id,
            displayTitle = displayTitle,
            subtitleTitle = subtitleTitle,
            imageUrl = anime.mainPictureLarge ?: anime.mainPictureMedium,
            mediaType = anime.mediaType?.uppercase(Locale.US),
            releaseSeason = releaseSeason,
            airingStatus = anime.airingStatus,
            score = anime.meanScore,
            totalEpisodes = anime.numEpisodes,
            parentAnimeId = parent.id,
            parentTitle = parentTitle,
            relationType = relation.relationType,
            relationTypeFormatted = relation.relationTypeFormatted,
            startSeasonYear = anime.startSeasonYear,
            startSeasonSeason = anime.startSeasonSeason,
        )
    }
}
