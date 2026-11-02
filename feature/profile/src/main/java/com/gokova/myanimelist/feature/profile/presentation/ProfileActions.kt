package com.gokova.myanimelist.feature.profile.presentation

data class AiSettingsActions(
    val onSaveKey: (String) -> Unit = {},
    val onClearKey: () -> Unit = {},
    val onTestKey: (String) -> Unit = {},
    val onShowInfoDialog: () -> Unit = {},
    val onDismissInfoDialog: () -> Unit = {},
)

data class ProfileActions(
    val onBackClick: () -> Unit = {},
    val onLogoutClick: () -> Unit = {},
    val onConfirmLogout: () -> Unit = {},
    val onDismissLogout: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
    val onExportClick: () -> Unit = {},
    val onImportClick: () -> Unit = {},
    val aiActions: AiSettingsActions = AiSettingsActions(),
)
