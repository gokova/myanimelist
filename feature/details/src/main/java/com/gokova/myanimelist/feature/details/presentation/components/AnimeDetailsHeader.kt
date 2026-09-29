package com.gokova.myanimelist.feature.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gokova.myanimelist.core.ui.theme.OnStatusCompletedContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusDroppedContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusOnHoldContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusPlanToWatchContainer
import com.gokova.myanimelist.core.ui.theme.OnStatusWatchingContainer
import com.gokova.myanimelist.core.ui.theme.StatusCompletedContainer
import com.gokova.myanimelist.core.ui.theme.StatusDroppedContainer
import com.gokova.myanimelist.core.ui.theme.StatusOnHoldContainer
import com.gokova.myanimelist.core.ui.theme.StatusPlanToWatchContainer
import com.gokova.myanimelist.core.ui.theme.StatusWatchingContainer
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.details.R
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import java.util.Locale

@Composable
fun AnimeDetailsHeader(
    details: AnimeDetails,
    isAddingToList: Boolean,
    onAddToList: () -> Unit,
    onPosterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.Top,
    ) {
        HeaderPoster(
            thumbnailUrl = details.mainPictureMedium,
            title = details.displayTitle,
            onPosterClick = onPosterClick,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            HeaderTitles(
                displayTitle = details.displayTitle,
                subtitleTitle = details.subtitleTitle,
            )

            HeaderScores(
                meanScore = details.meanScore,
                userScore = details.userScore,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            HeaderAction(
                isInUserList = details.isInUserList,
                userStatus = details.userStatus,
                isAddingToList = isAddingToList,
                onAddToList = onAddToList,
            )
        }
    }
}

@Composable
private fun HeaderPoster(
    thumbnailUrl: String?,
    title: String,
    onPosterClick: () -> Unit,
) {
    AsyncImage(
        model = thumbnailUrl,
        contentDescription =
            stringResource(
                R.string.details_zoom_poster_content_description,
                title,
            ),
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .width(110.dp)
                .height(165.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(role = Role.Button, onClick = onPosterClick),
    )
}

@Composable
private fun HeaderTitles(
    displayTitle: String,
    subtitleTitle: String?,
) {
    Text(
        text = displayTitle,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )

    if (subtitleTitle != null) {
        Text(
            text = subtitleTitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HeaderScores(
    meanScore: Double?,
    userScore: Int?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text =
                        meanScore?.let { String.format(Locale.US, "%.2f", it) }
                            ?: stringResource(R.string.details_score_empty),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        if (userScore != null) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.details_score_user, userScore),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderAction(
    isInUserList: Boolean,
    userStatus: String?,
    isAddingToList: Boolean,
    onAddToList: () -> Unit,
) {
    if (isInUserList) {
        val (containerColor, contentColor, labelRes) = getStatusColorAndLabel(userStatus)
        Surface(
            shape = MaterialTheme.shapes.small,
            color = containerColor,
        ) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    } else {
        Button(
            onClick = onAddToList,
            enabled = !isAddingToList,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.height(44.dp),
        ) {
            if (isAddingToList) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.details_adding_to_list))
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                Text(stringResource(R.string.details_add_to_list))
            }
        }
    }
}

private fun getStatusColorAndLabel(status: String?): Triple<Color, Color, Int> =
    when (status?.lowercase(Locale.US)) {
        "watching" ->
            Triple(
                StatusWatchingContainer,
                OnStatusWatchingContainer,
                R.string.details_status_watching,
            )
        "completed" ->
            Triple(
                StatusCompletedContainer,
                OnStatusCompletedContainer,
                R.string.details_status_completed,
            )
        "on_hold" ->
            Triple(
                StatusOnHoldContainer,
                OnStatusOnHoldContainer,
                R.string.details_status_on_hold,
            )
        "dropped" ->
            Triple(
                StatusDroppedContainer,
                OnStatusDroppedContainer,
                R.string.details_status_dropped,
            )
        else ->
            Triple(
                StatusPlanToWatchContainer,
                OnStatusPlanToWatchContainer,
                R.string.details_status_plan_to_watch,
            )
    }
