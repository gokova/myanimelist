package com.gokova.myanimelist.feature.details.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.details.R
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AnimeDetailsMetadata(
    details: AnimeDetails,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text(
            text = stringResource(R.string.details_information_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        MetadataBadgesFlowRow(details = details)
    }
}

@Composable
private fun MetadataBadgesFlowRow(
    details: AnimeDetails,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        BroadcastMetadataBadges(details = details)
        MetricMetadataBadges(details = details)
    }
}

@Composable
private fun BroadcastMetadataBadges(details: AnimeDetails) {
    details.mediaType?.let {
        MetadataBadge(
            label = stringResource(R.string.details_info_media_type),
            value = it.uppercase(Locale.US),
        )
    }
    details.status?.let {
        MetadataBadge(
            label = stringResource(R.string.details_info_status),
            value = formatAiringStatus(it),
        )
    }
    details.season?.let {
        MetadataBadge(
            label = stringResource(R.string.details_info_season),
            value = it,
        )
    }
    if (shouldShowEpisodes(details.mediaType, details.numEpisodes)) {
        MetadataBadge(
            label = stringResource(R.string.details_info_episodes),
            value = (details.numEpisodes ?: 0).toString(),
        )
    }
    formatDuration(details.durationSeconds)?.let {
        MetadataBadge(
            label = stringResource(R.string.details_info_duration),
            value = it,
        )
    }
}

@Composable
private fun MetricMetadataBadges(details: AnimeDetails) {
    details.rank?.let {
        MetadataBadge(
            label = stringResource(R.string.details_info_rank),
            value = stringResource(R.string.details_rank_format, it),
        )
    }
    details.popularity?.let {
        MetadataBadge(
            label = stringResource(R.string.details_info_popularity),
            value = stringResource(R.string.details_popularity_format, it),
        )
    }
    details.numListUsers?.let {
        MetadataBadge(
            label = stringResource(R.string.details_info_members),
            value = NumberFormat.getNumberInstance(Locale.US).format(it),
        )
    }
    details.numScoringUsers?.let {
        MetadataBadge(
            label = stringResource(R.string.details_info_scoring_users),
            value = NumberFormat.getNumberInstance(Locale.US).format(it),
        )
    }
}

@Composable
private fun MetadataBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun shouldShowEpisodes(
    mediaType: String?,
    numEpisodes: Int?,
): Boolean {
    if (numEpisodes == null || numEpisodes == 0) return false
    val isMovie = mediaType?.equals("movie", ignoreCase = true) == true
    val isMusic = mediaType?.equals("music", ignoreCase = true) == true
    return !(isMovie || isMusic)
}

@Composable
private fun formatAiringStatus(status: String): String =
    when (status.lowercase(Locale.US)) {
        "finished_airing" -> stringResource(R.string.details_airing_finished)
        "currently_airing" -> stringResource(R.string.details_airing_currently)
        "not_yet_aired" -> stringResource(R.string.details_airing_not_yet)
        else -> status.replace('_', ' ').replaceFirstChar { it.uppercase(Locale.US) }
    }

@Composable
private fun formatDuration(durationSeconds: Int?): String? {
    if (durationSeconds == null || durationSeconds <= 0) return null
    val totalMinutes = durationSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        if (minutes > 0) {
            stringResource(R.string.details_duration_hours_minutes, hours, minutes)
        } else {
            stringResource(R.string.details_duration_hours, hours)
        }
    } else {
        stringResource(R.string.details_duration_minutes, totalMinutes)
    }
}
