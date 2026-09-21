package com.gokova.myanimelist.feature.taste.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gokova.myanimelist.feature.taste.domain.model.TasteAnimeItem
import com.gokova.myanimelist.feature.taste.domain.model.TasteBubble
import com.gokova.myanimelist.feature.taste.domain.model.TasteType

class TasteUiStatePreviewParameterProvider : PreviewParameterProvider<TasteUiState> {
    private val sampleAnime =
        listOf(
            TasteAnimeItem(1, "Attack on Titan", null, null, 10, "completed", 25, 25),
            TasteAnimeItem(2, "Fullmetal Alchemist: Brotherhood", null, null, 9, "completed", 64, 64),
            TasteAnimeItem(3, "Demon Slayer", null, null, 8, "watching", 26, 15),
        )

    private val sampleGenreBubbles =
        listOf(
            TasteBubble("Action", 24, 75f, 0f, 0f, 0, 8.8, sampleAnime),
            TasteBubble("Comedy", 18, 62f, 120f, -20f, 1, 7.9, sampleAnime),
            TasteBubble("Drama", 15, 56f, -110f, 30f, 2, 8.4, sampleAnime),
            TasteBubble("Fantasy", 12, 50f, 30f, 110f, 3, 7.5, sampleAnime),
            TasteBubble("Romance", 8, 42f, -40f, -100f, 4, 8.0, sampleAnime),
            TasteBubble("Sci-Fi", 5, 36f, 100f, 90f, 5, 8.1, sampleAnime),
        )

    private val sampleThemeBubbles =
        listOf(
            TasteBubble("Shounen", 30, 82f, 0f, 0f, 6, 8.6, sampleAnime),
            TasteBubble("School", 16, 58f, -110f, -20f, 7, 7.8, sampleAnime),
            TasteBubble("Super Power", 14, 52f, 105f, 25f, 8, 8.2, sampleAnime),
            TasteBubble("Historical", 7, 40f, 20f, -100f, 9, 8.5, sampleAnime),
            TasteBubble("Isekai", 5, 36f, -30f, 100f, 10, 7.2, sampleAnime),
        )

    override val values: Sequence<TasteUiState> =
        sequenceOf(
            TasteUiState(
                isLoading = false,
                totalAnimeCount = 45,
                selectedType = TasteType.GENRE,
                genres = sampleGenreBubbles,
                themes = sampleThemeBubbles,
            ),
            TasteUiState(
                isLoading = false,
                totalAnimeCount = 45,
                selectedType = TasteType.THEME,
                genres = sampleGenreBubbles,
                themes = sampleThemeBubbles,
            ),
            TasteUiState(
                isLoading = false,
                totalAnimeCount = 0,
                selectedType = TasteType.GENRE,
            ),
            TasteUiState(
                isLoading = false,
                totalAnimeCount = 2,
                selectedType = TasteType.GENRE,
                genres = emptyList(),
                themes = emptyList(),
            ),
            TasteUiState(
                isLoading = true,
            ),
            TasteUiState(
                isLoading = false,
                totalAnimeCount = 45,
                selectedType = TasteType.GENRE,
                genres = sampleGenreBubbles,
                themes = sampleThemeBubbles,
                selectedBubble = sampleGenreBubbles.first(),
            ),
        )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            INDEX_GENRES -> "Genres Content"
            INDEX_THEMES -> "Themes Content"
            INDEX_EMPTY_LIST -> "Empty List"
            INDEX_EMPTY_DATA -> "Not Enough Data"
            INDEX_LOADING -> "Loading"
            INDEX_EXPANDED_BOTTOM_SHEET -> "Expanded Bottom Sheet"
            else -> null
        }

    companion object {
        private const val INDEX_GENRES = 0
        private const val INDEX_THEMES = 1
        private const val INDEX_EMPTY_LIST = 2
        private const val INDEX_EMPTY_DATA = 3
        private const val INDEX_LOADING = 4
        private const val INDEX_EXPANDED_BOTTOM_SHEET = 5
    }
}
