package com.gokova.myanimelist.feature.mylist.presentation.components

data class AnimeCardActions(
    val onPosterClick: (() -> Unit)? = null,
    val onCardClick: (() -> Unit)? = null,
    val onEditClick: (() -> Unit)? = null,
    val onQuickIncrement: (() -> Unit)? = null,
)
