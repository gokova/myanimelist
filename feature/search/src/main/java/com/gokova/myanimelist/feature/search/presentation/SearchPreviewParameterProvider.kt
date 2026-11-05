package com.gokova.myanimelist.feature.search.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gokova.myanimelist.feature.search.domain.model.AiSearchResult
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem

class SearchPreviewParameterProvider : PreviewParameterProvider<SearchUiState> {
    override val values: Sequence<SearchUiState> =
        sequenceOf(
            SearchUiState.Initial(),
            SearchUiState.Searching(query = "Frieren"),
            SearchUiState.Content(
                query = "Frieren",
                resultQuery = "Frieren",
                animes = sampleSearchAnimeList,
                hasMore = true,
                isLoadingMore = false,
            ),
            SearchUiState.Content(
                query = "Frieren Season 2",
                resultQuery = "Frieren",
                animes = sampleSearchAnimeList,
                hasMore = true,
                isLoadingMore = false,
                isReplacing = true,
                replacementQuery = "Frieren Season 2",
            ),
            SearchUiState.Empty(query = "NonexistentAnimeTitle"),
            SearchUiState.Error(
                query = "Attack on Titan",
                message = "Unable to connect to MyAnimeList",
            ),
            SearchUiState.AiInitial(),
            SearchUiState.AiSearching(aiPrompt = "Psychological thriller like Pluto"),
            SearchUiState.AiContent(
                aiPrompt = "Psychological thriller like Pluto",
                result =
                    AiSearchResult(
                        intentSummary = "Dark sci-fi mysteries",
                        explanation = "Top psychological anime",
                        items = sampleSearchAnimeList,
                    ),
            ),
            SearchUiState.AiEmpty(
                aiPrompt = "Obscure anime",
                intentSummary = "No matches",
                explanation = "Could not find fitting titles",
            ),
            SearchUiState.AiError(
                aiPrompt = "Action",
                message = "AI service unavailable",
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
            5 -> "AiInitial"
            6 -> "AiSearching"
            7 -> "AiContent"
            8 -> "AiEmpty"
            9 -> "AiError"
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
            id = 16498L,
            title = "Shingeki no Kyojin",
            englishTitle = "Attack on Titan",
            displayTitle = "Attack on Titan",
            subtitleTitle = "Shingeki no Kyojin",
            thumbnailUrl = null,
            mediaType = "tv",
            numEpisodes = 25,
            meanScore = 8.55,
            genres = listOf("Action", "Drama", "Suspense"),
            userStatus = "watching",
            tasteMatchPercent = 88,
        ),
        SearchAnimeItem(
            id = 99999L,
            title = "Untracked Anime",
            englishTitle = null,
            displayTitle = "Untracked Anime",
            subtitleTitle = null,
            thumbnailUrl = null,
            mediaType = "movie",
            numEpisodes = 1,
            meanScore = null,
            genres = emptyList(),
            userStatus = null,
            tasteMatchPercent = null,
        ),
    )
