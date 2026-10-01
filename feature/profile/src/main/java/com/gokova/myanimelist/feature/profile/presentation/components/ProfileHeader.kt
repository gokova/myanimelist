package com.gokova.myanimelist.feature.profile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.profile.R
import com.gokova.myanimelist.feature.profile.presentation.util.ProfileDateFormatter

private val AVATAR_SIZE = 96.dp
private val AVATAR_PLACEHOLDER_ICON_SIZE = 56.dp

@Composable
fun ProfileHeader(
    profile: UserProfile,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileAvatar(
            pictureUrl = profile.pictureUrl,
            username = profile.name,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Text(
            text = profile.name,
            style =
                MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
        )
        profile.joinedAt?.let { joinedAt ->
            val formattedDate = ProfileDateFormatter.formatJoinedDate(joinedAt)
            if (formattedDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                Text(
                    text = stringResource(R.string.profile_joined_format, formattedDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!profile.location.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(R.string.profile_location_format, profile.location!!),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileAvatar(
    pictureUrl: String?,
    username: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!pictureUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = pictureUrl,
                contentDescription =
                    stringResource(
                        R.string.profile_avatar_content_description,
                        username,
                    ),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(AVATAR_SIZE).clip(CircleShape),
                error = { AvatarPlaceholder() },
                loading = { AvatarPlaceholder() },
            )
        } else {
            AvatarPlaceholder()
        }
    }
}

@Composable
private fun AvatarPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(AVATAR_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(AVATAR_PLACEHOLDER_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
