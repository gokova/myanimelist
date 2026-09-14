package com.gokova.myanimelist.feature.taste.presentation

import com.gokova.myanimelist.feature.taste.domain.model.TasteBubble
import com.gokova.myanimelist.feature.taste.domain.model.TasteType

data class TasteActions(
    val onTypeSelected: (TasteType) -> Unit,
    val onBubbleClick: (TasteBubble) -> Unit,
    val onRecenterClick: () -> Unit,
    val onDismissBottomSheet: () -> Unit,
)
