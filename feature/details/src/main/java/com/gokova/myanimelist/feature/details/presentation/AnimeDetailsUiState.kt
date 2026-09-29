package com.gokova.myanimelist.feature.details.presentation

import androidx.annotation.StringRes
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails

data class AnimeDetailsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAddingToList: Boolean = false,
    val details: AnimeDetails? = null,
    @StringRes val errorMessageRes: Int? = null,
    val canRetry: Boolean = false,
)
