package com.gokova.myanimelist.feature.profile.presentation.components

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.profile.R
import com.gokova.myanimelist.feature.profile.presentation.AiKeyValidationStatus
import com.gokova.myanimelist.feature.profile.presentation.AiSettingsActions

private val SPINNER_SIZE = 16.dp

@Composable
fun AiSettingsCard(
    savedApiKey: String?,
    validationStatus: AiKeyValidationStatus,
    showInfoDialog: Boolean,
    actions: AiSettingsActions,
    modifier: Modifier = Modifier,
) {
    var inputKey by remember { mutableStateOf(savedApiKey ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(savedApiKey) {
        inputKey = savedApiKey ?: ""
    }

    Column(modifier = modifier.fillMaxWidth()) {
        AiSettingsCardHeader(onShowInfo = actions.onShowInfoDialog)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.medium),
            shape = MaterialTheme.shapes.medium,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium)) {
                Text(
                    text = stringResource(R.string.profile_ai_settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                AiKeyInputField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    passwordVisible = passwordVisible,
                    onToggleVisibility = { passwordVisible = !passwordVisible },
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                AiKeyActionRow(
                    inputKey = inputKey,
                    savedKey = savedApiKey,
                    validationStatus = validationStatus,
                    actions = actions,
                )
                AiKeyValidationStatusDisplay(status = validationStatus)
            }
        }
    }

    if (showInfoDialog) {
        AiKeyInfoDialog(onDismiss = actions.onDismissInfoDialog)
    }
}

@Composable
private fun AiSettingsCardHeader(
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            Text(
                text = stringResource(R.string.profile_ai_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(
            onClick = onShowInfo,
            modifier = Modifier.size(MaterialTheme.spacing.minTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.profile_ai_info_button_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiKeyInputField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
) {
    val visualTransformation =
        if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        }
    val icon =
        if (passwordVisible) {
            Icons.Default.Visibility
        } else {
            Icons.Default.VisibilityOff
        }
    val descriptionRes =
        if (passwordVisible) {
            R.string.profile_ai_hide_key_cd
        } else {
            R.string.profile_ai_show_key_cd
        }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.profile_ai_key_label)) },
        placeholder = { Text(stringResource(R.string.profile_ai_key_placeholder)) },
        singleLine = true,
        visualTransformation = visualTransformation,
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(descriptionRes),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AiKeyActionRow(
    inputKey: String,
    savedKey: String?,
    validationStatus: AiKeyValidationStatus,
    actions: AiSettingsActions,
) {
    val isTesting = validationStatus == AiKeyValidationStatus.Testing
    val hasChanged = inputKey.trim() != (savedKey ?: "")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = { actions.onSaveKey(inputKey) },
            enabled = inputKey.isNotBlank() && hasChanged,
        ) {
            Text(stringResource(R.string.profile_ai_save_key))
        }
        OutlinedButton(
            onClick = { actions.onTestKey(inputKey) },
            enabled = inputKey.isNotBlank() && !isTesting,
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SPINNER_SIZE),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.profile_ai_testing_key))
            } else {
                Text(stringResource(R.string.profile_ai_test_key))
            }
        }
        if (!savedKey.isNullOrBlank()) {
            TextButton(onClick = actions.onClearKey) {
                Text(stringResource(R.string.profile_ai_clear_key))
            }
        }
    }
}

@Composable
private fun AiKeyValidationStatusDisplay(status: AiKeyValidationStatus) {
    val errorResId =
        when (status) {
            AiKeyValidationStatus.Invalid -> R.string.profile_ai_key_invalid
            AiKeyValidationStatus.QuotaExceeded -> R.string.profile_ai_key_quota_exceeded
            AiKeyValidationStatus.ServiceUnavailable -> R.string.profile_ai_service_unavailable
            AiKeyValidationStatus.NetworkError -> R.string.profile_ai_network_error
            AiKeyValidationStatus.Idle,
            AiKeyValidationStatus.Testing,
            AiKeyValidationStatus.Valid,
            -> null
        }

    if (status == AiKeyValidationStatus.Valid) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(R.string.profile_ai_key_valid),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    } else if (errorResId != null) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(errorResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AiKeyInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_ai_info_dialog_title)) },
        text = { Text(stringResource(R.string.profile_ai_info_dialog_content)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_ai_info_dialog_dismiss))
            }
        },
    )
}
