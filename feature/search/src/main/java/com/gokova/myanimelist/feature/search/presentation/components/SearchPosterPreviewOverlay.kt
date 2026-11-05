package com.gokova.myanimelist.feature.search.presentation.components

import androidx.compose.runtime.Composable
import com.gokova.myanimelist.core.ui.component.AnimePosterPreviewDialog
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem

@Composable
fun SearchPosterPreviewOverlay(
    poster: SearchAnimeItem?,
    onDismiss: () -> Unit,
) {
    if (poster != null) {
        AnimePosterPreviewDialog(
            thumbnailUrl = poster.thumbnailUrl,
            largeImageUrl = poster.largeImageUrl,
            title = poster.displayTitle,
            onDismiss = onDismiss,
        )
    }
}
