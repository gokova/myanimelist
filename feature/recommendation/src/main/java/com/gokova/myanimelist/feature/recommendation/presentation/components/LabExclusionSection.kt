package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.domain.taxonomy.GENRE_NAMES
import com.gokova.myanimelist.core.domain.taxonomy.TagType
import com.gokova.myanimelist.core.domain.taxonomy.classifyTag
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.recommendation.R
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendationType

private val TOP_EXCLUSION_THEMES: List<String> =
    listOf(
        "Adult Cast",
        "CGDCT",
        "Combat Sports",
        "Detective",
        "Gag Humor",
        "Gore",
        "Harem",
        "High Stakes Game",
        "Historical",
        "Isekai",
        "Iyashikei",
        "Martial Arts",
        "Mecha",
        "Medical",
        "Military",
        "Music",
        "Mythology",
        "Otaku Culture",
        "Parody",
        "Psychological",
        "School",
        "Space",
        "Strategy Game",
        "Super Power",
        "Survival",
        "Time Travel",
        "Urban Fantasy",
        "Vampire",
        "Workplace",
    )

@Composable
fun LabExclusionSection(
    selectedType: RecommendationType,
    excludedTags: Set<String>,
    onTagToggled: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isGenre = selectedType == RecommendationType.GENRE
    val availableTags =
        remember(isGenre) {
            if (isGenre) {
                GENRE_NAMES.toList().sorted()
            } else {
                TOP_EXCLUSION_THEMES
            }
        }

    val titleRes =
        if (isGenre) {
            R.string.recommendation_lab_exclude_genres_title
        } else {
            R.string.recommendation_lab_exclude_themes_title
        }

    val currentTagType = if (isGenre) TagType.GENRE else TagType.THEME
    val otherTypeExclusions =
        remember(excludedTags, currentTagType) {
            excludedTags.filter { classifyTag(it) != currentTagType }.sorted()
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            availableTags.forEach { tag ->
                ExclusionChip(
                    tag = tag,
                    isExcluded = tag in excludedTags,
                    onToggle = onTagToggled,
                )
            }
        }
        if (otherTypeExclusions.isNotEmpty()) {
            OtherTabExclusions(
                otherTags = otherTypeExclusions,
                onRemove = onTagToggled,
            )
        }
    }
}

@Composable
private fun OtherTabExclusions(
    otherTags: List<String>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        Text(
            text = stringResource(R.string.recommendation_lab_other_tab_exclusions),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            otherTags.forEach { tag ->
                ExclusionChip(
                    tag = tag,
                    isExcluded = true,
                    onToggle = onRemove,
                )
            }
        }
    }
}

@Composable
private fun ExclusionChip(
    tag: String,
    isExcluded: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val descRes =
        if (isExcluded) {
            R.string.recommendation_lab_remove_exclusion_desc
        } else {
            R.string.recommendation_lab_exclude_tag_desc
        }
    val chipContentDescription = stringResource(descRes, tag)

    FilterChip(
        selected = isExcluded,
        onClick = { onToggle(tag) },
        label = { Text(text = tag) },
        trailingIcon =
            if (isExcluded) {
                {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                null
            },
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor =
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                selectedTrailingIconColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        modifier = modifier.semantics { contentDescription = chipContentDescription },
    )
}

@StandardPreviews
@Composable
private fun LabExclusionSectionPreview() {
    MyAnimeListTheme {
        LabExclusionSection(
            selectedType = RecommendationType.GENRE,
            excludedTags = setOf("Ecchi", "Isekai"),
            onTagToggled = {},
        )
    }
}
