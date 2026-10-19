package com.gokova.myanimelist.feature.mylist.presentation

sealed interface MyListUiEvent {
    data class ShowSnackbar(
        val messageRes: Int,
        val actionRes: Int? = null,
        val isError: Boolean = false,
    ) : MyListUiEvent

    data class ShowCompletionPrompt(
        val animeId: Long,
        val title: String,
    ) : MyListUiEvent

    data class ShowWatchingPrompt(
        val animeId: Long,
        val title: String,
    ) : MyListUiEvent
}
