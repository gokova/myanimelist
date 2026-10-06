package com.gokova.myanimelist.feature.search.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem

class SearchPreviewParameterProvider : PreviewParameterProvider<SearchUiState> {
    override val values: Sequence<SearchUiState> =
        sequenceOf(
            SearchUiState.Initial(),
            SearchUiState.Searching(query = "Frieren"),
            SearchUiState.Content(
                query = "Frieren",
                animes = sampleSearchAnimeList,
                hasMore = true,
                isLoadingMore = false,
            ),
            SearchUiState.Empty(query = "NonexistentAnimeTitle"),
            SearchUiState.Error(
                query = "Attack on Titan",
                message = "Unable to connect to MyAnimeList",
            ),
        )

    @Suppress("MagicNumber")
    override fun getDisplayName(index: Int): String? =
        when (index) {
            0 -> "Initial"
            1 -> "Searching"
            2 -> "Content"
            3 -> "Empty"
            4 -> "Error"
            else -> null
        }
}

class SearchAnimeItemPreviewParameterProvider : PreviewParameterProvider<SearchAnimeItem> {
    override val values: Sequence<SearchAnimeItem> = sampleSearchAnimeList.asSequence()
}

val sampleSearchAnimeList =
    listOf(
        SearchAnimeItem(
            id = 52991L,
            title = "Sousou no Frieren",
            englishTitle = "Frieren: Beyond Journey's End",
            displayTitle = "Frieren: Beyond Journey's End",
            subtitleTitle = "Sousou no Frieren",
            thumbnailUrl = null,
            mediaType = "tv",
            numEpisodes = 28,
            meanScore = 9.38,
            genres = listOf("Adventure", "Drama", "Fantasy"),
            userStatus = "completed",
            tasteMatchPercent = 94,
        ),
        SearchAnimeItem(
            id = 5114L,
            title = "Fullmetal Alchemist: Brotherhood",
            englishTitle = "Fullmetal Alchemist: Brotherhood",
            displayTitle = "Fullmetal Alchemist: Brotherhood",
            subtitleTitle = null,
            thumbnailUrl = null,
            mediaType = "tv",
            numEpisodes = 64,
            meanScore = 9.10,
            genres = listOf("Action", "Adventure", "Drama", "Fantasy"),
            userStatus = null,
            tasteMatchPercent = 88,
        ),
        SearchAnimeItem(
            id = 26243L,
            title = "Akame ga Kill! Recap",
            englishTitle = "Akame ga Kill! Recap",
            displayTitle = "Akame ga Kill! Recap",
            subtitleTitle = null,
            thumbnailUrl = null,
            mediaType = "tv_special",
            numEpisodes = 1,
            meanScore = 6.70,
            genres = listOf("Action", "Fantasy", "Gore"),
            userStatus = null,
            tasteMatchPercent = 99,
        ),
        SearchAnimeItem(
            id = 21L,
            title = "One Piece",
            englishTitle = "One Piece",
            displayTitle = "One Piece",
            subtitleTitle = null,
            thumbnailUrl = null,
            mediaType = "tv",
            numEpisodes = 0,
            meanScore = 8.72,
            genres = listOf("Action", "Adventure", "Fantasy"),
            userStatus = "watching",
            tasteMatchPercent = 75,
        ),
    )
