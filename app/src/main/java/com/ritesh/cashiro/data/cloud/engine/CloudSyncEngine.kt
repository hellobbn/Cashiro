package com.ritesh.cashiro.data.cloud.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ritesh.cashiro.data.backup.BackupConfiguration
import com.ritesh.cashiro.data.backup.BackupExporter
import com.ritesh.cashiro.data.backup.BackupImporter
import com.ritesh.cashiro.data.backup.ExportResult
import com.ritesh.cashiro.data.backup.ImportResult
import com.ritesh.cashiro.data.backup.ImportStrategy
import com.ritesh.cashiro.data.cloud.CloudSyncResult
import com.ritesh.cashiro.data.cloud.security.CloudCredentialStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine implementing differential multi-device synchronization modeled on Cashew (`syncClient.dart`).
 */
@Singleton
class CloudSyncEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter,
    private val cloudBackupManager: CloudBackupManager,
    private val cloudCredentialStore: CloudCredentialStore,
    private val encryptionEngine: com.ritesh.cashiro.data.cloud.security.BackupEncryptionEngine
) {
    /**
     * With end-to-end encryption on, sync snapshots are encrypted like backups: the whole ledger
     * must never sit in the sync folder in plain text. Null passphrase: encryption is off.
     */
    private fun passphrase(): String? =
        if (cloudCredentialStore.isE2eEncryptionEnabled()) cloudCredentialStore.getE2ePassphrase() else null

    /** The snapshot to upload: encrypted (".enc") when E2E is on, else the zip itself. */
    private fun sealed(snapshot: File, passphrase: String?): File {
        if (passphrase == null) return snapshot
        val encrypted = File(context.cacheDir, "${snapshot.nameWithoutExtension}.enc")
        val result = encryptionEngine.encryptFile(snapshot, encrypted, passphrase)
        snapshot.delete()
        return result.getOrNull() ?: throw IllegalStateException("Failed to encrypt the sync snapshot.")
    }


    companion object {
        const val SYNC_FOLDER = "cashiro_sync"
    }

    /**
     * Perform differential synchronization across devices via the active cloud provider.
     */
    suspend fun synchronize(progressListener: ((Int) -> Unit)? = null): Result<CloudSyncResult> = withContext(Dispatchers.IO) {
        val (provider, providerConfig) = cloudBackupManager.getActiveProvider()
            ?: return@withContext Result.failure(IllegalStateException("No active or configured cloud storage provider."))

        try {
            val deviceId = cloudCredentialStore.getDeviceId()
            val passphrase = passphrase()
            if (passphrase != null && passphrase.isBlank()) {
                return@withContext Result.failure(IllegalStateException("End-to-end encryption is on but no passphrase is set; set it before syncing."))
            }
            val extension = if (passphrase != null) "enc" else "zip"
            val remoteSyncPath = "$SYNC_FOLDER/sync-$deviceId.$extension"

            progressListener?.invoke(10)
            // Step 1: Export local state snapshot and publish/upload to sync folder
            val exportResult = backupExporter.exportBackup(BackupConfiguration())
            if (exportResult !is ExportResult.Success) {
                val errorMsg = if (exportResult is ExportResult.Error) exportResult.message else "Sync local export failed"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val localSnapshotFile = sealed(exportResult.file, passphrase)
            val uploadResult = provider.uploadFile(localSnapshotFile, remoteSyncPath, providerConfig) { upProg ->
                progressListener?.invoke(10 + (upProg * 25 / 100))
            }

            if (localSnapshotFile.exists()) localSnapshotFile.delete()

            if (uploadResult.isFailure) {
                return@withContext Result.failure(uploadResult.exceptionOrNull() ?: Exception("Failed to publish local sync state."))
            }

            // A plain snapshot left from before encryption was turned on must not stay behind
            if (passphrase != null) provider.deleteFile("$SYNC_FOLDER/sync-$deviceId.zip", providerConfig)

            progressListener?.invoke(40)

            // Step 2: List remote sync snapshots and discover peer devices
            val listResult = provider.listFiles(SYNC_FOLDER, providerConfig)
            if (listResult.isFailure) {
                return@withContext Result.failure(listResult.exceptionOrNull() ?: Exception("Failed to list remote sync files."))
            }

            val allSyncFiles = listResult.getOrNull() ?: emptyList()
            val peerSyncFiles = allSyncFiles.filter {
                it.name.startsWith("sync-") && (it.name.endsWith(".zip") || it.name.endsWith(".enc")) && !it.name.contains(deviceId)
            }

            var peersSynced = 0
            var totalTransactionsImported = 0
            var totalCategoriesImported = 0
            var anyChangesMerged = false

            val cacheDir = File(context.cacheDir, "peer_syncs")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // Step 3: Compare timestamps and merge new peer updates
            for ((index, peerFile) in peerSyncFiles.withIndex()) {
                val peerId = peerFile.name.removePrefix("sync-").removeSuffix(".zip").removeSuffix(".enc")
                val lastSyncedTime = cloudCredentialStore.getPeerLastSyncedTimestamp(peerId)

                // If remote peer file is newer than when we last synced with this peer
                if (peerFile.lastModified > lastSyncedTime) {
                    val peerDestFile = File(cacheDir, "peer_$peerId.zip")
                    val downResult = provider.downloadFile(peerFile.path, peerDestFile, providerConfig)
                    val importFile = if (downResult.isSuccess && peerDestFile.exists() &&
                        (peerFile.name.endsWith(".enc") || encryptionEngine.isEncryptedBackup(peerDestFile))
                    ) {
                        // An encrypted peer needs the same passphrase; without it the peer is skipped
                        val plain = File(cacheDir, "peer_$peerId.plain.zip")
                        val ok = !passphrase.isNullOrBlank() &&
                            encryptionEngine.decryptFile(peerDestFile, plain, passphrase).isSuccess && plain.exists()
                        peerDestFile.delete()
                        if (ok) plain else null
                    } else peerDestFile.takeIf { downResult.isSuccess && it.exists() }
                    if (importFile != null) {
                        val importResult = backupImporter.importBackup(Uri.fromFile(importFile), ImportStrategy.MERGE)
                        if (importResult is ImportResult.Success) {
                            totalTransactionsImported += importResult.importedTransactions
                            totalCategoriesImported += importResult.importedCategories
                            if (importResult.importedTransactions > 0 || importResult.importedCategories > 0) {
                                anyChangesMerged = true
                            }
                            peersSynced++
                            cloudCredentialStore.setPeerLastSyncedTimestamp(peerId, peerFile.lastModified)
                        }
                        importFile.delete()
                    }
                }
                val currentProgress = 40 + ((index + 1) * 45 / peerSyncFiles.size.coerceAtLeast(1))
                progressListener?.invoke(currentProgress)
            }

            // Step 4: If we merged incoming peer updates into our local database, re-publish our updated state
            if (anyChangesMerged) {
                val updatedExport = backupExporter.exportBackup(BackupConfiguration())
                if (updatedExport is ExportResult.Success) {
                    val sealedUpdate = sealed(updatedExport.file, passphrase)
                    provider.uploadFile(sealedUpdate, remoteSyncPath, providerConfig)
                    if (sealedUpdate.exists()) sealedUpdate.delete()
                }
            }

            cloudCredentialStore.setLastSyncTimestamp(System.currentTimeMillis())
            progressListener?.invoke(100)

            Result.success(
                CloudSyncResult(
                    peersSynced = peersSynced,
                    importedTransactions = totalTransactionsImported,
                    importedCategories = totalCategoriesImported
                )
            )
        } catch (e: Exception) {
            Log.e("CloudSyncEngine", "Synchronization failed", e)
            Result.failure(e)
        }
    }
}
