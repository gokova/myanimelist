package com.gokova.myanimelist.feature.mylist.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.mylist.R
import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory

private fun getTabTitleRes(category: ListFilterCategory): Int =
    when (category) {
        ListFilterCategory.ALL -> R.string.my_list_tab_all
        ListFilterCategory.WATCHING -> R.string.my_list_tab_watching
        ListFilterCategory.COMPLETED -> R.string.my_list_tab_completed
        ListFilterCategory.ON_HOLD -> R.string.my_list_tab_on_hold
        ListFilterCategory.DROPPED -> R.string.my_list_tab_dropped
        ListFilterCategory.PLAN_TO_WATCH -> R.string.my_list_tab_plan_to_watch
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListCategoryTabs(
    selectedCategory: ListFilterCategory,
    onCategorySelected: (ListFilterCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = ListFilterCategory.entries
    val selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = MaterialTheme.spacing.medium,
        divider = {},
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            Tab(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                modifier =
                    Modifier.heightIn(min = MaterialTheme.spacing.minTouchTarget),
                text = {
                    Text(
                        text = stringResource(getTabTitleRes(category)),
                        style = MaterialTheme.typography.labelLarge,
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraSmall),
                    )
                },
            )
        }
    }
}

@StandardPreviews
@Composable
private fun MyListCategoryTabsPreview() {
    MyAnimeListTheme {
        MyListCategoryTabs(
            selectedCategory = ListFilterCategory.WATCHING,
            onCategorySelected = {},
        )
    }
}
