package com.gokova.myanimelist.feature.taste.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.component.badge.PersonalScoreBadge
import com.gokova.myanimelist.core.ui.theme.TasteColors
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.taste.R
import com.gokova.myanimelist.feature.taste.domain.model.TasteBubble
import com.gokova.myanimelist.feature.taste.domain.model.TasteType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasteCategoryListBottomSheet(
    bubbles: List<TasteBubble>,
    selectedType: TasteType,
    onBubbleClick: (TasteBubble) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sortedBubbles = remember(bubbles) { bubbles.sortedByDescending { it.count } }
    val isDark = isSystemInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.spacing.large),
        ) {
            HeaderSection(
                selectedType = selectedType,
                count = bubbles.size,
                onDismiss = onDismissRequest,
            )

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .padding(horizontal = MaterialTheme.spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                items(sortedBubbles, key = { it.name }) { bubble ->
                    CategoryItemRow(
                        bubble = bubble,
                        isDark = isDark,
                        onClick = {
                            onDismissRequest()
                            onBubbleClick(bubble)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    selectedType: TasteType,
    count: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.spacing.screenHorizontal,
                    end = MaterialTheme.spacing.small,
                    bottom = MaterialTheme.spacing.small,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            val titleRes =
                if (selectedType == TasteType.GENRE) {
                    R.string.taste_tab_genres
                } else {
                    R.string.taste_tab_themes
                }
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.taste_categories_sheet_title) + " ($count)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.taste_categories_sheet_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryItemRow(
    bubble: TasteBubble,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TasteColors.getColorScheme(bubble.colorIndex, isDark)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.small,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Box(
            modifier =
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(colors.container),
        )

        Text(
            text = bubble.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text =
                pluralStringResource(
                    R.plurals.taste_bottom_sheet_entries,
                    bubble.count,
                    bubble.count,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PersonalScoreBadge(score = bubble.averageScore)
    }
}
