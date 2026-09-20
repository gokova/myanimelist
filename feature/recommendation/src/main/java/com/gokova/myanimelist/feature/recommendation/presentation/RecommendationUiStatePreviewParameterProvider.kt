package com.gokova.myanimelist.feature.recommendation.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime

class RecommendationUiStatePreviewParameterProvider :
    PreviewParameterProvider<RecommendationUiState> {
    private val sampleAnime =
        listOf(
            RecommendedAnime(
                animeId = 1L,
                title = "Frieren: Beyond Journey's End",
                titleEnglish = "Frieren: Beyond Journey's End",
                imageUrl = null,
                mediaType = "tv",
                airingStatus = "finished_airing",
                numEpisodes = 28,
                startSeasonYear = 2023,
                startSeasonSeason = "fall",
                meanScore = 9.38,
                genreRank = 1,
                genreScore = 4.8,
                genreMatchPercent = 98,
                themeRank = 2,
                themeScore = 4.2,
                themeMatchPercent = 94,
                genres = listOf("Adventure", "Drama", "Fantasy", "Magic"),
                studios = listOf("Madhouse"),
                source = "manga",
            ),
            RecommendedAnime(
                animeId = 2L,
                title = "Steins;Gate",
                titleEnglish = "Steins;Gate",
                imageUrl = null,
                mediaType = "tv",
                airingStatus = "finished_airing",
                numEpisodes = 24,
                startSeasonYear = 2011,
                startSeasonSeason = "spring",
                meanScore = 9.07,
                genreRank = 2,
                genreScore = 4.6,
                genreMatchPercent = 95,
                themeRank = 1,
                themeScore = 4.7,
                themeMatchPercent = 97,
                genres = listOf("Sci-Fi", "Suspense", "Psychological", "Time Travel"),
                studios = listOf("White Fox"),
                source = "visual_novel",
            ),
        )

    override val values: Sequence<RecommendationUiState> =
        sequenceOf(
            RecommendationUiState.Success(
                selectedType = RecommendationType.GENRE,
                recommendations = sampleAnime,
            ),
            RecommendationUiState.Success(
                selectedType = RecommendationType.THEME,
                recommendations = sampleAnime,
            ),
            RecommendationUiState.EmptyInsufficientData,
            RecommendationUiState.Calculating,
            RecommendationUiState.Loading,
        )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            INDEX_GENRES -> "Genre Recommendations"
            INDEX_THEMES -> "Theme Recommendations"
            INDEX_INSUFFICIENT_DATA -> "Insufficient Data"
            INDEX_CALCULATING -> "Calculating"
            INDEX_LOADING -> "Loading"
            else -> null
        }

    companion object {
        private const val INDEX_GENRES = 0
        private const val INDEX_THEMES = 1
        private const val INDEX_INSUFFICIENT_DATA = 2
        private const val INDEX_CALCULATING = 3
        private const val INDEX_LOADING = 4
    }
}
