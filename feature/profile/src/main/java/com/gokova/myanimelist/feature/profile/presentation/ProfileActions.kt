package com.gokova.myanimelist.feature.profile.presentation

data class ProfileActions(
    val onBackClick: () -> Unit = {},
    val onLogoutClick: () -> Unit = {},
    val onConfirmLogout: () -> Unit = {},
    val onDismissLogout: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
)
