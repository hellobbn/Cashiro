package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.update.GitHubRelease

@Composable
fun GitHubUpdateDialog(
    release: GitHubRelease,
    onDismiss: () -> Unit,
    onDownload: (GitHubRelease) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_available_title)) },
        text = {
            Text(stringResource(R.string.update_available_body, release.title))
        },
        confirmButton = {
            TextButton(onClick = { onDownload(release) }) {
                Text(stringResource(R.string.update_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_later))
            }
        }
    )
}
