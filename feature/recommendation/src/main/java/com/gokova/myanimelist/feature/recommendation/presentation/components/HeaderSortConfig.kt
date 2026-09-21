package com.gokova.myanimelist.feature.recommendation.presentation.components

import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption

data class HeaderSortConfig(
    val selectedSort: NewSeasonSortOption,
    val onSortSelected: (NewSeasonSortOption) -> Unit,
)
