package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import com.ritesh.cashiro.data.backup.BackupConfiguration
import java.io.File

data class DataPrivacyUiState(
    val importExportMessage: String? = null,
    val exportedBackupFile: File? = null,
    val backupConfiguration: BackupConfiguration = BackupConfiguration(),
    val hasNewAccountsCreated: Boolean = false
)
