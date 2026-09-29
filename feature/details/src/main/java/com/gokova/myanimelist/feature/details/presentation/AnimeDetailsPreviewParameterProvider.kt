package com.gokova.myanimelist.feature.details.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gokova.myanimelist.feature.details.R
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.domain.model.DetailRecommendation
import com.gokova.myanimelist.feature.details.domain.model.RelatedAnime

@Suppress("MagicNumber")
class AnimeDetailsPreviewParameterProvider : PreviewParameterProvider<AnimeDetailsUiState> {
    private val inListDetails =
        AnimeDetails(
            id = 1,
            displayTitle = "Attack on Titan",
            subtitleTitle = "Shingeki no Kyojin",
            originalTitle = "Shingeki no Kyojin",
            englishTitle = "Attack on Titan",
            mainPictureMedium = null,
            mainPictureLarge = null,
            synopsis = "Centuries ago, mankind was nearly slaughtered to extinction...",
            genres = listOf("Action", "Drama", "Suspense"),
            themes = listOf("Military", "Survival", "Gore"),
            meanScore = 8.54,
            userScore = 9,
            userStatus = "completed",
            isInUserList = true,
            rank = 110,
            popularity = 1,
            numListUsers = 3800000,
            numScoringUsers = 2700000,
            mediaType = "tv",
            status = "finished_airing",
            season = "2013 Spring",
            numEpisodes = 25,
            durationSeconds = 1440,
            relatedAnime =
                listOf(
                    RelatedAnime(
                        id = 2,
                        title = "Attack on Titan Season 2",
                        thumbnailUrl = null,
                        relationType = "sequel",
                        relationTypeFormatted = "Sequel",
                    ),
                ),
            recommendations =
                listOf(
                    DetailRecommendation(
                        id = 3,
                        title = "Kabaneri of the Iron Fortress",
                        thumbnailUrl = null,
                        meanScore = 7.26,
                        numRecommendations = 150,
                    ),
                ),
        )

    private val newAnimeDetails =
        AnimeDetails(
            id = 4,
            displayTitle = "Frieren: Beyond Journey's End",
            subtitleTitle = "Sousou no Frieren",
            originalTitle = "Sousou no Frieren",
            englishTitle = "Frieren: Beyond Journey's End",
            mainPictureMedium = null,
            mainPictureLarge = null,
            synopsis = "During their decade-long quest to defeat the Demon King...",
            genres = listOf("Adventure", "Drama", "Fantasy"),
            themes = listOf("Magic"),
            meanScore = 9.38,
            userScore = null,
            userStatus = null,
            isInUserList = false,
            rank = 1,
            popularity = 50,
            numListUsers = 850000,
            numScoringUsers = 520000,
            mediaType = "tv",
            status = "finished_airing",
            season = "2023 Fall",
            numEpisodes = 28,
            durationSeconds = 1440,
            relatedAnime = emptyList(),
            recommendations =
                listOf(
                    DetailRecommendation(
                        id = 5,
                        title = "Violet Evergarden",
                        thumbnailUrl = null,
                        meanScore = 8.68,
                        numRecommendations = 95,
                    ),
                ),
        )

    override val values: Sequence<AnimeDetailsUiState> =
        sequenceOf(
            AnimeDetailsUiState(
                isLoading = false,
                details = newAnimeDetails,
            ),
            AnimeDetailsUiState(
                isLoading = false,
                details = inListDetails,
            ),
            AnimeDetailsUiState(
                isLoading = false,
                isAddingToList = true,
                details = newAnimeDetails,
            ),
            AnimeDetailsUiState(
                isLoading = true,
                details = null,
            ),
            AnimeDetailsUiState(
                isLoading = false,
                details = null,
                errorMessageRes = R.string.details_error_not_found,
                canRetry = true,
            ),
        )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            INDEX_NEW_ANIME -> "New Anime (Add to List)"
            INDEX_IN_LIST -> "In User List"
            INDEX_ADDING_TO_LIST -> "Adding to List"
            INDEX_LOADING -> "Loading"
            INDEX_ERROR -> "Error"
            else -> null
        }

    companion object {
        private const val INDEX_NEW_ANIME = 0
        private const val INDEX_IN_LIST = 1
        private const val INDEX_ADDING_TO_LIST = 2
        private const val INDEX_LOADING = 3
        private const val INDEX_ERROR = 4
    }
}
