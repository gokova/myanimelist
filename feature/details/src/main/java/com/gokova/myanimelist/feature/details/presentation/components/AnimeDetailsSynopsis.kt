package com.gokova.myanimelist.feature.details.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.details.R

private const val COLLAPSED_MAX_LINES = 5
private const val SYNOPSIS_EXPANDABLE_THRESHOLD = 200

@Composable
fun AnimeDetailsSynopsis(
    synopsis: String?,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text(
            text = stringResource(R.string.details_synopsis_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        val displayText =
            synopsis?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.details_no_synopsis)

        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.animateContentSize(),
        )

        if (synopsis != null && synopsis.length > SYNOPSIS_EXPANDABLE_THRESHOLD) {
            Text(
                text =
                    stringResource(
                        if (isExpanded) {
                            R.string.details_synopsis_show_less
                        } else {
                            R.string.details_synopsis_read_more
                        },
                    ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(role = Role.Button) { isExpanded = !isExpanded },
            )
        }
    }
}
