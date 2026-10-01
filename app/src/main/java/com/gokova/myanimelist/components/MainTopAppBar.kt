package com.gokova.myanimelist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.gokova.myanimelist.R
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing

private val AVATAR_SIZE = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.main_app_bar_title),
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    ),
            )
        },
        actions = {
            AvatarButton(
                onClick = onAvatarClick,
                avatarUrl = avatarUrl,
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
    )
}

@Composable
private fun AvatarButton(
    onClick: () -> Unit,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(end = MaterialTheme.spacing.small)
                .size(MaterialTheme.spacing.minTouchTarget)
                .clip(CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = avatarUrl,
                    contentDescription =
                        stringResource(
                            R.string.main_avatar_content_description,
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
}

@Composable
private fun AvatarPlaceholder(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription =
            stringResource(
                R.string.main_avatar_content_description,
            ),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@StandardPreviews
@Composable
internal fun MainTopAppBarPreview() {
    MyAnimeListTheme {
        MainTopAppBar(
            onAvatarClick = {},
            avatarUrl = null,
        )
    }
}
