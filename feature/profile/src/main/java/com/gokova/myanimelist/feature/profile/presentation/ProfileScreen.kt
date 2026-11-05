package com.gokova.myanimelist.feature.profile.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
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
import com.gokova.myanimelist.feature.profile.presentation.components.AiSettingsCard
import com.gokova.myanimelist.feature.profile.presentation.components.IgnoredAnimeSection
import com.gokova.myanimelist.feature.profile.presentation.components.LogoutButton
import com.gokova.myanimelist.feature.profile.presentation.components.ProfileHeader
import com.gokova.myanimelist.feature.profile.presentation.components.ProfileStatsSection
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DETAILS_COLUMN_WEIGHT = 1.3f

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogoutConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri ->
            if (uri != null) {
                viewModel.exportIgnoredAnime { context.contentResolver.openOutputStream(uri) }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.importIgnoredAnime { context.contentResolver.openInputStream(uri) }
            }
        }

    HandleProfileEvents(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onLogoutConfirm = onLogoutConfirm,
    )

    ProfileScreen(
        uiState = uiState,
        actions =
            ProfileActions(
                onBackClick = viewModel::onBackClicked,
                onLogoutClick = { viewModel.setLogoutDialogVisible(true) },
                onConfirmLogout = viewModel::onLogoutConfirmed,
                onDismissLogout = { viewModel.setLogoutDialogVisible(false) },
                onRetryClick = viewModel::refresh,
                onExportClick = {
                    val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                    exportLauncher.launch("mal_ignored_anime_$dateStr.csv")
                },
                onImportClick = {
                    importLauncher.launch(
                        arrayOf("text/csv", "text/comma-separated-values", "text/*", "*/*"),
                    )
                },
                aiActions =
                    AiSettingsActions(
                        onSaveKey = viewModel::saveAiApiKey,
                        onClearKey = viewModel::clearAiApiKey,
                        onTestKey = viewModel::testAiApiKey,
                        onShowInfoDialog = { viewModel.setAiInfoDialogVisible(true) },
                        onDismissInfoDialog = { viewModel.setAiInfoDialogVisible(false) },
                    ),
            ),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
private fun HandleProfileEvents(
    viewModel: ProfileViewModel,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onLogoutConfirm: () -> Unit,
) {
    val resources = LocalResources.current
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ProfileUiEvent.NavigateBack -> onBackClick()
                is ProfileUiEvent.RequestLogout -> onLogoutConfirm()
                is ProfileUiEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(resources.getString(event.messageResId))
                }
                is ProfileUiEvent.ShowExportSuccessSnackbar -> {
                    snackbarHostState.showSnackbar(
                        resources.getString(
                            R.string.profile_ignored_export_success,
                            event.count,
                        ),
                    )
                }
                is ProfileUiEvent.ShowImportSuccessSnackbar -> {
                    snackbarHostState.showSnackbar(
                        resources.getString(
                            R.string.profile_ignored_import_success,
                            event.totalCount,
                            event.newCount,
                            event.updatedCount,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                stringResource(R.string.profile_back_content_description),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (uiState) {
                is ProfileUiState.Loading -> ProfileLoadingState()
                is ProfileUiState.Content ->
                    ProfileContentState(
                        content = uiState,
                        actions = actions,
                    )
                is ProfileUiState.Error ->
                    ProfileErrorState(
                        messageResId = uiState.messageResId,
                        onRetryClick = actions.onRetryClick,
                    )
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
    messageResId: Int,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val screenHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
    val isTwoColumn =
        screenWidthDp >= 600.dp || (screenWidthDp > screenHeightDp && screenHeightDp < 500.dp)

    Box(modifier = modifier.fillMaxSize()) {
        if (isTwoColumn) {
            ProfileLandscapeContent(
                content = content,
                actions = actions,
            )
        } else {
            ProfilePortraitContent(
                content = content,
                actions = actions,
            )
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
private fun ProfilePortraitContent(
    content: ProfileUiState.Content,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
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
        IgnoredAnimeSection(
            ignoredCount = content.ignoredAnimeCount,
            onExportClick = actions.onExportClick,
            onImportClick = actions.onImportClick,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        AiSettingsCard(
            savedApiKey = content.aiApiKey,
            validationStatus = content.aiKeyValidationStatus,
            showInfoDialog = content.showAiInfoDialog,
            actions = actions.aiActions,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        LogoutButton(onClick = actions.onLogoutClick)
    }
}

@Composable
private fun ProfileLandscapeContent(
    content: ProfileUiState.Content,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileHeader(profile = content.userProfile)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            LogoutButton(onClick = actions.onLogoutClick)
        }

        Column(
            modifier =
                Modifier
                    .weight(DETAILS_COLUMN_WEIGHT)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content.userProfile.statistics?.let { stats ->
                ProfileStatsSection(statistics = stats)
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            }
            IgnoredAnimeSection(
                ignoredCount = content.ignoredAnimeCount,
                onExportClick = actions.onExportClick,
                onImportClick = actions.onImportClick,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            AiSettingsCard(
                savedApiKey = content.aiApiKey,
                validationStatus = content.aiKeyValidationStatus,
                showInfoDialog = content.showAiInfoDialog,
                actions = actions.aiActions,
            )
        }
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
