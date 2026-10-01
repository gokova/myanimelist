package com.gokova.myanimelist.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gokova.myanimelist.core.ui.component.LogoutConfirmationDialog
import com.gokova.myanimelist.core.ui.component.LogoutConfirmationDialogContent
import com.gokova.myanimelist.core.ui.preview.StandardPreviews
import com.gokova.myanimelist.core.ui.theme.MyAnimeListTheme
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.profile.R
import com.gokova.myanimelist.feature.profile.presentation.components.ProfileHeader
import com.gokova.myanimelist.feature.profile.presentation.components.ProfileStatsSection
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogoutConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ProfileUiEvent.NavigateBack -> onBackClick()
                is ProfileUiEvent.RequestLogout -> onLogoutConfirm()
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        actions =
            ProfileActions(
                onBackClick = viewModel::onBackClicked,
                onLogoutClick = viewModel::onLogoutClicked,
                onConfirmLogout = viewModel::onLogoutConfirmed,
                onDismissLogout = viewModel::onLogoutDismissed,
                onRetryClick = viewModel::refresh,
            ),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_screen_title),
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                stringResource(R.string.profile_back_content_description),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when (uiState) {
                is ProfileUiState.Loading -> ProfileLoadingState()
                is ProfileUiState.Error ->
                    ProfileErrorState(uiState.messageResId, actions.onRetryClick)
                is ProfileUiState.Content -> ProfileContentState(uiState, actions)
            }
        }
    }
}

@Composable
private fun ProfileLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ProfileErrorState(
    @androidx.annotation.StringRes messageResId: Int,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Button(
            onClick = onRetryClick,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(text = stringResource(R.string.profile_retry_button))
        }
    }
}

@Composable
private fun ProfileContentState(
    content: ProfileUiState.Content,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileHeader(profile = content.userProfile)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            content.userProfile.statistics?.let { stats ->
                ProfileStatsSection(statistics = stats)
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            }
            LogoutButton(onClick = actions.onLogoutClick)
        }

        if (content.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            )
        }

        if (content.showLogoutDialog) {
            ProfileLogoutDialogOverlay(actions = actions)
        }
    }
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .height(48.dp),
        shape = MaterialTheme.shapes.large,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = null,
            modifier = Modifier.padding(end = MaterialTheme.spacing.small),
        )
        Text(
            text = stringResource(R.string.profile_logout_button),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun ProfileLogoutDialogOverlay(actions: ProfileActions) {
    if (LocalInspectionMode.current) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            LogoutConfirmationDialogContent(
                onConfirm = actions.onConfirmLogout,
                onDismiss = actions.onDismissLogout,
            )
        }
    } else {
        LogoutConfirmationDialog(
            onConfirm = actions.onConfirmLogout,
            onDismiss = actions.onDismissLogout,
        )
    }
}

@StandardPreviews
@Composable
fun ProfileScreenPreview(
    @PreviewParameter(ProfileUiStatePreviewParameterProvider::class)
    uiState: ProfileUiState,
) {
    MyAnimeListTheme {
        ProfileScreen(
            uiState = uiState,
            actions = ProfileActions(),
        )
    }
}
