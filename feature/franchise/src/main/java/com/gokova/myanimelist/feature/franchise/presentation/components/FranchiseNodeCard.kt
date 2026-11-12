package com.gokova.myanimelist.feature.franchise.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.gokova.myanimelist.feature.franchise.R
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNode

@Composable
fun FranchiseNodeCard(
    node: FranchiseNode,
    onClick: (FranchiseNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (node.isOrigin) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    val borderWidth = if (node.isOrigin) 2.dp else 1.dp
    val alphaModifier = if (node.isNotInterested) Modifier.alpha(0.55f) else Modifier

    val localizedStatus =
        node.userStatus?.let { getStatusLabel(it) }
            ?: stringResource(R.string.franchise_node_status_not_in_list)

    val a11yDescription =
        stringResource(
            R.string.franchise_accessibility_node,
            node.displayTitle,
            if (node.isOrigin) stringResource(R.string.franchise_node_origin_badge) else "",
            node.formattedMediaType,
            localizedStatus,
        )

    Card(
        modifier =
            modifier
                .then(alphaModifier)
                .semantics { contentDescription = a11yDescription }
                .clickable { onClick(node) },
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FranchiseNodePoster(posterUrl = node.posterMedium)

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                FranchiseNodeTitles(node = node)
                FranchiseNodeBadges(node = node)
            }
        }
    }
}

@Composable
private fun FranchiseNodePoster(
    posterUrl: String?,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = posterUrl,
        contentDescription = null,
        modifier =
            modifier
                .width(48.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun FranchiseNodeTitles(
    node: FranchiseNode,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (node.isOrigin) {
            Text(
                text = stringResource(R.string.franchise_node_origin_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            )
        }
        Text(
            text = node.displayTitle,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun FranchiseNodeBadges(
    node: FranchiseNode,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(
                text = node.formattedMediaType,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (node.isNotInterested) {
            Icon(
                imageVector = Icons.Default.VisibilityOff,
                contentDescription = stringResource(R.string.franchise_node_not_interested),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp),
            )
        } else if (node.isInUserList && node.userStatus != null) {
            val (container, content) = getStatusColors(node.userStatus)
            Surface(
                modifier = Modifier.weight(1f, fill = false),
                color = container,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = getStatusLabel(node.userStatus),
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun getStatusColors(status: String): Pair<Color, Color> =
    when (status.lowercase()) {
        "watching" -> StatusWatchingContainer to OnStatusWatchingContainer
        "completed" -> StatusCompletedContainer to OnStatusCompletedContainer
        "on_hold" -> StatusOnHoldContainer to OnStatusOnHoldContainer
        "dropped" -> StatusDroppedContainer to OnStatusDroppedContainer
        else -> StatusPlanToWatchContainer to OnStatusPlanToWatchContainer
    }

@Composable
private fun getStatusLabel(status: String): String =
    when (status.lowercase()) {
        "watching" -> stringResource(com.gokova.myanimelist.core.ui.R.string.status_watching)
        "completed" -> stringResource(com.gokova.myanimelist.core.ui.R.string.status_completed)
        "on_hold" -> stringResource(com.gokova.myanimelist.core.ui.R.string.status_on_hold)
        "dropped" -> stringResource(com.gokova.myanimelist.core.ui.R.string.status_dropped)
        "plan_to_watch" ->
            stringResource(com.gokova.myanimelist.core.ui.R.string.status_plan_to_watch)
        else -> status.replaceFirstChar { it.uppercase() }
    }
