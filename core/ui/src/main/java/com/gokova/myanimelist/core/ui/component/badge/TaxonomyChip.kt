package com.gokova.myanimelist.core.ui.component.badge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing

/**
 * Standard taxonomy tag for anime genres and themes across cards and detail pages.
 * Uses [MaterialTheme.colorScheme.secondaryContainer] (soft slate blue)
 * with [MaterialTheme.shapes.small] (8.dp radius).
 */
@Composable
fun TaxonomyChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier =
        if (onClick != null) {
            Modifier.clickable(role = Role.Button, onClick = onClick)
        } else {
            Modifier
        }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.then(clickableModifier),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            softWrap = false,
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = 2.dp,
                ),
        )
    }
}

@StandardPreviews
@Composable
private fun TaxonomyChipPreview() {
    MyAnimeListTheme {
        TaxonomyChip(
            text = "Action",
            modifier = Modifier.padding(16.dp),
        )
    }
}
