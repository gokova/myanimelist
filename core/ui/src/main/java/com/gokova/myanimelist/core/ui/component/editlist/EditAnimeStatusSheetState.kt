package com.gokova.myanimelist.core.ui.component.editlist

data class EditAnimeStatusSheetState(
    val animeId: Long = 0L,
    val title: String = "",
    val status: String = "plan_to_watch",
    val episodesWatched: Int = 0,
    val totalEpisodes: Int? = null,
    val score: Int = 0,
    val isRewatching: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val initialStatus: String = status,
    val initialEpisodesWatched: Int = episodesWatched,
) {
    val isMutating: Boolean get() = isSaving || isDeleting
}

data class EditAnimeStatusSheetActions(
    val onStatusChange: (String) -> Unit,
    val onEpisodesChange: (Int) -> Unit,
    val onScoreChange: (Int) -> Unit,
    val onRewatchingChange: (Boolean) -> Unit,
    val onSave: () -> Unit,
    val onRequestDelete: () -> Unit,
    val onConfirmDelete: () -> Unit,
    val onDismissDeleteConfirmation: () -> Unit,
    val onDismiss: () -> Unit,
)

fun EditAnimeStatusSheetState.withStatus(newStatus: String): EditAnimeStatusSheetState {
    val total = totalEpisodes
    val isKnownTotal = total != null && total > 0
    val newEpisodes =
        if (newStatus.equals("completed", ignoreCase = true) && isKnownTotal) {
            total
        } else {
            episodesWatched
        }
    return copy(status = newStatus, episodesWatched = newEpisodes)
}
