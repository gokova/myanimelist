package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.text.style.TextOverflow
import com.gokova.myanimelist.feature.recommendation.R
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType

@Composable
fun RecommendationPillSelector(
    selectedType: RecommendationType,
    onTypeSelected: (RecommendationType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val types = RecommendationType.entries
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.height(IntrinsicSize.Min),
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
                modifier = Modifier.fillMaxHeight(),
                icon = {},
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                label = {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}
