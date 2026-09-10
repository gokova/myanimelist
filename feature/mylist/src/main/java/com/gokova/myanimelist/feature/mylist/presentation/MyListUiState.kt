package com.gokova.myanimelist.feature.mylist.presentation

import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SortOption
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime

data class MyListUiState(
    val animeList: List<UserAnime> = emptyList(),
    val selectedCategory: ListFilterCategory = ListFilterCategory.ALL,
    val selectedSort: SortOption = SortOption.SCORE_DESC,
    val isSyncing: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingInitial: Boolean = true,
)
