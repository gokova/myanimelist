package com.gokova.myanimelist.feature.mylist.presentation.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gokova.myanimelist.feature.mylist.domain.model.AiringStatus
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnimeStatus

class UserAnimePreviewParameterProvider : PreviewParameterProvider<UserAnime> {
    private val animeList =
        listOf(
            UserAnime(
                id = 1L,
                originalTitle = "One Piece",
                englishTitle = "One Piece",
                displayTitle = "One Piece",
                subtitleTitle = "ワンピース",
                imageUrl = null,
                mediaType = "TV",
                airingStatus = AiringStatus.CURRENTLY_AIRING,
                releaseSeason = "1999 Fall",
                totalEpisodes = null,
                userStatus = UserAnimeStatus.WATCHING,
                userScore = 9,
                watchedEpisodes = 195,
                isRewatching = false,
                updatedAt = "2023-08-01",
            ),
            UserAnime(
                id = 2L,
                originalTitle = "Boku dake ga Inai Machi",
                englishTitle = "ERASED",
                displayTitle = "ERASED",
                subtitleTitle = "Boku dake ga Inai Machi",
                imageUrl = null,
                mediaType = "TV",
                airingStatus = AiringStatus.FINISHED_AIRING,
                releaseSeason = "2016 Winter",
                totalEpisodes = 12,
                userStatus = UserAnimeStatus.COMPLETED,
                userScore = 10,
                watchedEpisodes = 12,
                isRewatching = false,
                updatedAt = "2023-08-02",
            ),
            UserAnime(
                id = 3L,
                originalTitle = "Sousou no Frieren",
                englishTitle = "Frieren: Beyond Journey's End",
                displayTitle = "Frieren: Beyond Journey's End",
                subtitleTitle = "Sousou no Frieren",
                imageUrl = null,
                mediaType = "TV",
                airingStatus = AiringStatus.FINISHED_AIRING,
                releaseSeason = "2023 Fall",
                totalEpisodes = 28,
                userStatus = UserAnimeStatus.PLAN_TO_WATCH,
                userScore = 0,
                watchedEpisodes = 0,
                isRewatching = false,
                updatedAt = "2023-10-01",
            ),
            UserAnime(
                id = 4L,
                originalTitle = "Steins;Gate",
                englishTitle = "Steins;Gate",
                displayTitle = "Steins;Gate",
                subtitleTitle = null,
                imageUrl = null,
                mediaType = "TV",
                airingStatus = AiringStatus.FINISHED_AIRING,
                releaseSeason = "2011 Spring",
                totalEpisodes = 24,
                userStatus = UserAnimeStatus.ON_HOLD,
                userScore = 8,
                watchedEpisodes = 12,
                isRewatching = false,
                updatedAt = "2023-06-15",
            ),
            UserAnime(
                id = 5L,
                originalTitle = "Ex-Arm",
                englishTitle = "Ex-Arm",
                displayTitle = "Ex-Arm",
                subtitleTitle = null,
                imageUrl = null,
                mediaType = "TV",
                airingStatus = AiringStatus.FINISHED_AIRING,
                releaseSeason = "2021 Winter",
                totalEpisodes = 12,
                userStatus = UserAnimeStatus.DROPPED,
                userScore = 3,
                watchedEpisodes = 2,
                isRewatching = false,
                updatedAt = "2022-01-10",
            ),
        )

    override val values: Sequence<UserAnime> = animeList.asSequence()

    override fun getDisplayName(index: Int): String? =
        animeList.getOrNull(index)?.let { anime ->
            when (anime.userStatus) {
                UserAnimeStatus.WATCHING -> "Watching"
                UserAnimeStatus.COMPLETED -> "Completed"
                UserAnimeStatus.PLAN_TO_WATCH -> "Plan to Watch"
                UserAnimeStatus.ON_HOLD -> "On Hold"
                UserAnimeStatus.DROPPED -> "Dropped"
            }
        }
}
