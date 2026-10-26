package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.componentSizes
import com.gokova.myanimelist.feature.recommendation.R
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType

private val COMPACT_SELECTOR_HEIGHT = 36.dp

@Composable
fun RecommendationPillSelector(
    selectedType: RecommendationType,
    onTypeSelected: (RecommendationType) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val types = RecommendationType.entries
    val selectorHeight =
        if (compact) {
            COMPACT_SELECTOR_HEIGHT
        } else {
            MaterialTheme.componentSizes.segmentedSelectorHeight
        }
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.height(selectorHeight),
    ) {
        types.forEachIndexed { index, type ->
            val labelRes =
                when (type) {
                    RecommendationType.GENRE -> R.string.recommendation_tab_genres
                    RecommendationType.THEME -> R.string.recommendation_tab_themes
                    RecommendationType.NEW_SEASONS -> R.string.recommendation_tab_new_seasons
                }

            SegmentedButton(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = {},
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                label = {
                    val labelStyle =
                        if (compact) {
                            MaterialTheme.typography.labelMedium
                        } else {
                            MaterialTheme.typography.labelLarge
                        }
                    Text(
                        text = stringResource(labelRes),
                        style = labelStyle,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                },
            )
        }
    }
}
