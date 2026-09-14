package com.gokova.myanimelist.feature.taste.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gokova.myanimelist.feature.taste.R
import com.gokova.myanimelist.feature.taste.domain.model.TasteType

@Composable
fun TasteTypeSelector(
    selectedType: TasteType,
    onTypeSelected: (TasteType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val types = TasteType.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        types.forEachIndexed { index, type ->
            val labelRes =
                when (type) {
                    TasteType.GENRE -> R.string.taste_tab_genres
                    TasteType.THEME -> R.string.taste_tab_themes
                }
            SegmentedButton(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                label = {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}
