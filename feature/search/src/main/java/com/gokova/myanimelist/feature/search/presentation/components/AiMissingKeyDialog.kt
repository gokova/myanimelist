package com.gokova.myanimelist.feature.search.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gokova.myanimelist.feature.search.R
import com.gokova.myanimelist.feature.search.presentation.MissingKeyActions

@Composable
fun AiMissingKeyDialog(actions: MissingKeyActions) {
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(stringResource(R.string.ai_search_missing_key_title)) },
        text = { Text(stringResource(R.string.ai_search_missing_key_message)) },
        confirmButton = {
            Button(onClick = actions.onConfigureKey) {
                Text(stringResource(R.string.ai_search_missing_key_configure))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismiss) {
                Text(stringResource(R.string.ai_search_missing_key_cancel))
            }
        },
    )
}
