package com.gokova.myanimelist.feature.mylist.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gokova.myanimelist.feature.mylist.domain.model.AiringStatus
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SortOption
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnimeStatus

class MyListUiStatePreviewParameterProvider : PreviewParameterProvider<MyListUiState> {
    private val sampleAnimeList =
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
        )

    override val values: Sequence<MyListUiState> =
        sequenceOf(
            // 1. Populated list state
            MyListUiState(
                animeList = sampleAnimeList,
                selectedCategory = ListFilterCategory.ALL,
                selectedSort = SortOption.SCORE_DESC,
                isSyncing = false,
                isRefreshing = false,
                isLoadingInitial = false,
            ),
            // 2. Empty state (0 entries)
            MyListUiState(
                animeList = emptyList(),
                selectedCategory = ListFilterCategory.COMPLETED,
                selectedSort = SortOption.SCORE_DESC,
                isSyncing = false,
                isRefreshing = false,
                isLoadingInitial = false,
            ),
            // 3. Syncing / Refreshing state
            MyListUiState(
                animeList = sampleAnimeList,
                selectedCategory = ListFilterCategory.WATCHING,
                selectedSort = SortOption.SCORE_DESC,
                isSyncing = true,
                isRefreshing = true,
                isLoadingInitial = false,
            ),
        )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            0 -> "Loaded"
            1 -> "Empty"
            2 -> "Syncing"
            else -> null
        }
}
