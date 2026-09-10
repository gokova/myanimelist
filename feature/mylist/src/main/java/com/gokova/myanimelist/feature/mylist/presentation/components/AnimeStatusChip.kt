package com.gokova.myanimelist.feature.mylist.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
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
import com.gokova.myanimelist.feature.mylist.R
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnimeStatus

private data class StatusStyle(
    val containerColor: Color,
    val contentColor: Color,
    val labelRes: Int,
    val icon: ImageVector,
)

private fun getStatusStyle(status: UserAnimeStatus): StatusStyle =
    when (status) {
        UserAnimeStatus.WATCHING ->
            StatusStyle(
                containerColor = StatusWatchingContainer,
                contentColor = OnStatusWatchingContainer,
                labelRes = R.string.my_list_status_watching,
                icon = Icons.Default.PlayArrow,
            )
        UserAnimeStatus.COMPLETED ->
            StatusStyle(
                containerColor = StatusCompletedContainer,
                contentColor = OnStatusCompletedContainer,
                labelRes = R.string.my_list_status_completed,
                icon = Icons.Default.Check,
            )
        UserAnimeStatus.ON_HOLD ->
            StatusStyle(
                containerColor = StatusOnHoldContainer,
                contentColor = OnStatusOnHoldContainer,
                labelRes = R.string.my_list_status_on_hold,
                icon = Icons.Default.Pause,
            )
        UserAnimeStatus.DROPPED ->
            StatusStyle(
                containerColor = StatusDroppedContainer,
                contentColor = OnStatusDroppedContainer,
                labelRes = R.string.my_list_status_dropped,
                icon = Icons.Default.Close,
            )
        UserAnimeStatus.PLAN_TO_WATCH ->
            StatusStyle(
                containerColor = StatusPlanToWatchContainer,
                contentColor = OnStatusPlanToWatchContainer,
                labelRes = R.string.my_list_status_plan_to_watch,
                icon = Icons.Default.Bookmark,
            )
    }

@Composable
fun AnimeStatusChip(
    status: UserAnimeStatus,
    modifier: Modifier = Modifier,
) {
    val style = getStatusStyle(status)
    Row(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(style.containerColor)
                .padding(
                    horizontal = MaterialTheme.spacing.badgePaddingHorizontal,
                    vertical = MaterialTheme.spacing.badgePaddingVertical,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = style.contentColor,
            modifier = Modifier.size(MaterialTheme.spacing.iconSmall),
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
        Text(
            text = stringResource(style.labelRes),
            color = style.contentColor,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

class UserAnimeStatusPreviewParameterProvider : PreviewParameterProvider<UserAnimeStatus> {
    override val values: Sequence<UserAnimeStatus> = UserAnimeStatus.entries.asSequence()

    override fun getDisplayName(index: Int): String? =
        UserAnimeStatus.entries.getOrNull(index)?.let { status ->
            when (status) {
                UserAnimeStatus.WATCHING -> "Watching"
                UserAnimeStatus.COMPLETED -> "Completed"
                UserAnimeStatus.ON_HOLD -> "On Hold"
                UserAnimeStatus.DROPPED -> "Dropped"
                UserAnimeStatus.PLAN_TO_WATCH -> "Plan to Watch"
            }
        }
}

@StandardPreviews
@Composable
private fun AnimeStatusChipPreview(
    @PreviewParameter(UserAnimeStatusPreviewParameterProvider::class)
    status: UserAnimeStatus,
) {
    MyAnimeListTheme {
        AnimeStatusChip(status = status)
    }
}
