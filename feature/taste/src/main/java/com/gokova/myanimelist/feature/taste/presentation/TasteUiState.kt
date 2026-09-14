package com.gokova.myanimelist.feature.taste.presentation

import com.gokova.myanimelist.feature.taste.domain.model.TasteBubble
import com.gokova.myanimelist.feature.taste.domain.model.TasteType

data class TasteUiState(
    val isLoading: Boolean = true,
    val totalAnimeCount: Int = 0,
    val selectedType: TasteType = TasteType.GENRE,
    val genres: List<TasteBubble> = emptyList(),
    val themes: List<TasteBubble> = emptyList(),
    val selectedBubble: TasteBubble? = null,
    val recenterTrigger: Int = 0,
) {
    val currentBubbles: List<TasteBubble>
        get() = if (selectedType == TasteType.GENRE) genres else themes

    val isEmptyList: Boolean
        get() = !isLoading && totalAnimeCount == 0

    val isEmptyEligible: Boolean
        get() = !isLoading && totalAnimeCount > 0 && currentBubbles.isEmpty()
}
