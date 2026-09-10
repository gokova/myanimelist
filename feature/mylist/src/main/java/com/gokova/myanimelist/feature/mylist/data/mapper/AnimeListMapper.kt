package com.gokova.myanimelist.feature.mylist.data.mapper

import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.feature.mylist.domain.model.AiringStatus
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnimeStatus
import java.util.Locale

object AnimeListMapper {
    fun toAnimeEntity(dto: AnimeListEntryDto): AnimeEntity {
        val node = dto.node
        val englishTitle = node.alternativeTitles?.en?.takeIf { it.isNotBlank() }
        return AnimeEntity(
            id = node.id,
            title = node.title,
            titleEnglish = englishTitle,
            mainPictureMedium = node.mainPicture?.medium,
            mainPictureLarge = node.mainPicture?.large,
            mediaType = node.mediaType,
            airingStatus = node.status,
            numEpisodes = node.numEpisodes,
            startSeasonYear = node.startSeason?.year,
            startSeasonSeason = node.startSeason?.season,
            meanScore = node.mean,
        )
    }

    fun toUserAnimeListEntity(dto: AnimeListEntryDto): UserAnimeListEntity {
        val statusDto = dto.listStatus
        return UserAnimeListEntity(
            animeId = dto.node.id,
            status = statusDto?.status ?: UserAnimeStatus.PLAN_TO_WATCH.apiValue,
            score = statusDto?.score ?: 0,
            numEpisodesWatched = statusDto?.numEpisodesWatched ?: 0,
            updatedAt = statusDto?.updatedAt ?: "",
            isRewatching = statusDto?.isRewatching ?: false,
        )
    }

    fun toDomain(item: UserAnimeListItem): UserAnime {
        val anime = item.anime
        val userStatus = item.userAnime
        val englishTitle = anime.titleEnglish?.takeIf { it.isNotBlank() }
        val displayTitle = englishTitle ?: anime.title
        val subtitleTitle =
            if (englishTitle != null && !englishTitle.equals(anime.title, ignoreCase = true)) {
                anime.title
            } else {
                null
            }

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

        return UserAnime(
            id = anime.id,
            originalTitle = anime.title,
            englishTitle = englishTitle,
            displayTitle = displayTitle,
            subtitleTitle = subtitleTitle,
            imageUrl = anime.mainPictureLarge ?: anime.mainPictureMedium,
            mediaType = anime.mediaType?.uppercase(Locale.US),
            airingStatus = AiringStatus.fromApiValue(anime.airingStatus),
            releaseSeason = releaseSeason,
            totalEpisodes = anime.numEpisodes,
            userStatus = UserAnimeStatus.fromApiValue(userStatus.status),
            userScore = userStatus.score,
            watchedEpisodes = userStatus.numEpisodesWatched,
            isRewatching = userStatus.isRewatching,
            updatedAt = userStatus.updatedAt,
        )
    }
}
