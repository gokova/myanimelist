package com.gokova.myanimelist.feature.recommendation.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gokova.myanimelist.feature.recommendation.domain.model.DurationFitTier
import com.gokova.myanimelist.feature.recommendation.domain.model.PopularityTier
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationScoreBreakdown
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
                thumbnailUrl = null,
                largeImageUrl = null,
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
                breakdown =
                    RecommendationScoreBreakdown(
                        tasteAlignmentScore = 0.98f,
                        discoveryScore = 0.12f,
                        topMatchedTags = listOf("Adventure", "Drama", "Fantasy"),
                        exploratoryTags = listOf("Magic"),
                        durationTier = DurationFitTier.StandardCour(28),
                        popularityTier = PopularityTier.MAINSTREAM_FAVORITE,
                    ),
            ),
            RecommendedAnime(
                animeId = 2L,
                title = "Steins;Gate",
                titleEnglish = "Steins;Gate",
                thumbnailUrl = null,
                largeImageUrl = null,
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
                breakdown =
                    RecommendationScoreBreakdown(
                        tasteAlignmentScore = 0.95f,
                        discoveryScore = 0.20f,
                        topMatchedTags = listOf("Sci-Fi", "Suspense"),
                        exploratoryTags = listOf("Time Travel"),
                        durationTier = DurationFitTier.StandardCour(24),
                        popularityTier = PopularityTier.POPULAR_PICK,
                    ),
            ),
        )

    private val sampleNewSeason =
        listOf(
            com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime(
                animeId = 30230L,
                displayTitle = "Diamond no Ace: Second Season",
                subtitleTitle = "Daiya no Ace: Second Season",
                thumbnailUrl = null,
                largeImageUrl = null,
                mediaType = "TV",
                releaseSeason = "2015 Spring",
                airingStatus = "finished_airing",
                score = 8.42,
                totalEpisodes = 51,
                parentAnimeId = 18689L,
                parentTitle = "Diamond no Ace",
                relationType = "sequel",
                relationTypeFormatted = "Sequel",
                startSeasonYear = 2015,
                startSeasonSeason = "spring",
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
            RecommendationUiState.Success(
                selectedType = RecommendationType.NEW_SEASONS,
                newSeasons = sampleNewSeason,
            ),
            RecommendationUiState.Success(
                selectedType = RecommendationType.GENRE,
                recommendations = sampleAnime,
                isLabOpen = true,
                labPreviewCount = 142,
            ),
            RecommendationUiState.EmptyInsufficientData(),
            RecommendationUiState.EmptyAllCaughtUp(),
            RecommendationUiState.Calculating(),
            RecommendationUiState.Loading(),
            RecommendationUiState.Error(message = "Preview error"),
        )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            INDEX_GENRES -> "Genre Recommendations"
            INDEX_THEMES -> "Theme Recommendations"
            INDEX_NEW_SEASONS -> "New Seasons"
            INDEX_LAB_OPEN -> "Lab Open"
            INDEX_INSUFFICIENT_DATA -> "Insufficient Data"
            INDEX_ALL_CAUGHT_UP -> "All Caught Up"
            INDEX_CALCULATING -> "Calculating"
            INDEX_LOADING -> "Loading"
            INDEX_ERROR -> "Error"
            else -> null
        }

    companion object {
        private const val INDEX_GENRES = 0
        private const val INDEX_THEMES = 1
        private const val INDEX_NEW_SEASONS = 2
        private const val INDEX_LAB_OPEN = 3
        private const val INDEX_INSUFFICIENT_DATA = 4
        private const val INDEX_ALL_CAUGHT_UP = 5
        private const val INDEX_CALCULATING = 6
        private const val INDEX_LOADING = 7
        private const val INDEX_ERROR = 8
    }
}
