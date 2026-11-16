package com.gokova.myanimelist.feature.details.presentation.components

import com.gokova.myanimelist.core.domain.model.PendingAnimeMutation

data class AnimeDetailsHeaderState(
    val isAddingToList: Boolean = false,
    val isNotInterested: Boolean = false,
    val pendingMutation: PendingAnimeMutation? = null,
)
