package com.gokova.myanimelist.feature.profile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.domain.model.UserAnimeStatistics
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.DarkStatusChartColors
import com.gokova.myanimelist.core.ui.theme.LightStatusChartColors
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.StatusChartColors
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.profile.R

private val STAR_ICON_SIZE = 16.dp
private val METRIC_CARD_MIN_HEIGHT = 64.dp
private val DISTRIBUTION_BAR_HEIGHT = 12.dp
private val DISTRIBUTION_BAR_CORNER_RADIUS = 6.dp
private val STATUS_DOT_SIZE = 10.dp
private const val PERCENTAGE_MULTIPLIER = 100f
private const val DARK_THEME_LUMINANCE_THRESHOLD = 0.5f

@Composable
fun ProfileStatsSection(
    statistics: UserAnimeStatistics,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.profile_anime_stats_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        PrimaryMetricsRow(statistics = statistics)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        StatusDistributionCard(
            statistics = statistics,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
        )
    }
}

@Composable
private fun PrimaryMetricsRow(
    statistics: UserAnimeStatistics,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        MetricCard(
            label = stringResource(R.string.profile_stat_days_watched),
            value = stringResource(R.string.profile_days_format, statistics.numDaysWatched),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        MetricCard(
            label = stringResource(R.string.profile_stat_mean_score),
            value = stringResource(R.string.profile_score_format, statistics.meanScore),
            leadingIcon = Icons.Default.Star,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        MetricCard(
            label = stringResource(R.string.profile_stat_total_entries),
            value = stringResource(R.string.profile_count_format, statistics.numItems),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        MetricCard(
            label = stringResource(R.string.profile_stat_episodes),
            value = stringResource(R.string.profile_count_format, statistics.numEpisodes),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    Card(
        modifier = modifier.defaultMinSize(minHeight = METRIC_CARD_MIN_HEIGHT),
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = MaterialTheme.spacing.extraSmall,
                        vertical = MaterialTheme.spacing.small,
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(STAR_ICON_SIZE).padding(end = 2.dp),
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class StatusDistributionItem(
    val labelRes: Int,
    val count: Int,
    val color: Color,
)

private fun buildStatusItems(
    statistics: UserAnimeStatistics,
    colors: StatusChartColors,
): List<StatusDistributionItem> =
    listOf(
        StatusDistributionItem(
            labelRes = R.string.profile_status_watching,
            count = statistics.numItemsWatching,
            color = colors.watching,
        ),
        StatusDistributionItem(
            labelRes = R.string.profile_status_completed,
            count = statistics.numItemsCompleted,
            color = colors.completed,
        ),
        StatusDistributionItem(
            labelRes = R.string.profile_status_on_hold,
            count = statistics.numItemsOnHold,
            color = colors.onHold,
        ),
        StatusDistributionItem(
            labelRes = R.string.profile_status_dropped,
            count = statistics.numItemsDropped,
            color = colors.dropped,
        ),
        StatusDistributionItem(
            labelRes = R.string.profile_status_plan_to_watch,
            count = statistics.numItemsPlanToWatch,
            color = colors.planToWatch,
        ),
    )

@Composable
private fun StatusDistributionCard(
    statistics: UserAnimeStatistics,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < DARK_THEME_LUMINANCE_THRESHOLD
    val chartColors = if (isDark) DarkStatusChartColors else LightStatusChartColors
    val items = buildStatusItems(statistics, chartColors)
    val totalCount = items.sumOf { it.count }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
        ) {
            Text(
                text = stringResource(R.string.profile_status_distribution_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            SegmentedDistributionBar(items = items, totalCount = totalCount)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            items.forEach { item ->
                val percentage =
                    if (totalCount > 0) {
                        (item.count.toFloat() / totalCount) * PERCENTAGE_MULTIPLIER
                    } else {
                        0f
                    }
                StatusDistributionRow(
                    label = stringResource(item.labelRes),
                    count = item.count,
                    percentage = percentage,
                    indicatorColor = item.color,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
            StatusDistributionRow(
                label = stringResource(R.string.profile_stat_rewatched),
                count = statistics.numTimesRewatched,
                percentage = null,
                indicatorColor = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun SegmentedDistributionBar(
    items: List<StatusDistributionItem>,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(DISTRIBUTION_BAR_CORNER_RADIUS)
    val trackBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    if (totalCount == 0) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(DISTRIBUTION_BAR_HEIGHT)
                    .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = barShape)
                    .border(width = 1.dp, color = trackBorder, shape = barShape),
        )
    } else {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(DISTRIBUTION_BAR_HEIGHT)
                    .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = barShape)
                    .border(width = 1.dp, color = trackBorder, shape = barShape)
                    .clip(barShape)
                    .clearAndSetSemantics { },
        ) {
            items.forEach { item ->
                if (item.count > 0) {
                    Box(
                        modifier =
                            Modifier
                                .weight(item.count.toFloat())
                                .fillMaxHeight()
                                .background(item.color),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDistributionRow(
    label: String,
    count: Int,
    percentage: Float?,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(STATUS_DOT_SIZE)
                    .background(color = indicatorColor, shape = CircleShape)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = CircleShape,
                    ),
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        Text(
            text = stringResource(R.string.profile_count_format, count),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (percentage != null) {
            Text(
                text = stringResource(R.string.profile_percentage_format, percentage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = MaterialTheme.spacing.extraSmall),
            )
        }
    }
}

@StandardPreviews
@Composable
private fun ProfileStatsSectionPreview() {
    MyAnimeListTheme {
        Surface {
            ProfileStatsSection(
                statistics =
                    UserAnimeStatistics(
                        numItemsWatching = 1,
                        numItemsCompleted = 61,
                        numItemsOnHold = 0,
                        numItemsDropped = 0,
                        numItemsPlanToWatch = 10,
                        numItems = 72,
                        numDaysWatched = 43.6f,
                        numDays = 43.6f,
                        numEpisodes = 2587,
                        numTimesRewatched = 0,
                        meanScore = 8.28f,
                    ),
            )
        }
    }
}
