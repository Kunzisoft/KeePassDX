/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.action

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.keeshare.DeviceIdentity
import com.kunzisoft.keepass.database.keeshare.KeeShareExport
import com.kunzisoft.keepass.database.keeshare.KeeShareImport
import com.kunzisoft.keepass.database.keeshare.PerDeviceSyncConfig
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import java.io.File
import java.io.InputStream

class KeeShareSyncRunnable(
    context: Context,
    database: ContextualDatabase,
    saveDatabase: Boolean,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    private val progressTaskUpdater: ProgressTaskUpdater?
) : SaveDatabaseRunnable(
    context,
    database,
    saveDatabase,
    null,
    challengeResponseRetriever
) {

    var importedEntries: Int = 0
    var importedDevices: Int = 0
    var exportedEntries: Int = 0

    override fun onStartRun() {
        database.wasReloaded = true
        super.onStartRun()
    }

    override fun onActionRun() {
        try {
            val kdbx = database.databaseKDBX
            if (kdbx == null) {
                setError("KeeShare sync requires a KDBX database")
                return
            }

            val deviceId = resolveDeviceId(context)
            val cacheDir = File(context.cacheDir, "keeshare")
            cacheDir.mkdirs()

            // 1. Import from all other device containers via SAF
            val importResults = KeeShareImport.importAll(
                database = kdbx,
                ownDeviceId = deviceId,
                cacheDirectory = cacheDir,
                fileProvider = { syncDirUri, ownId ->
                    listOtherDeviceStreams(context, syncDirUri, ownId)
                },
                isRAMSufficient = { memoryWanted ->
                    BinaryData.canMemoryBeAllocatedInRAM(context, memoryWanted)
                }
            )

            importedEntries = importResults.filter { it.success }.sumOf { it.entriesImported }
            importedDevices = importResults.filter { it.success }
                .map { it.containerName }.distinct().size

            // 2. Export own container for each shared group via SAF
            val exportResults = KeeShareExport.exportAll(
                database = kdbx,
                deviceId = deviceId,
                cacheDirectory = cacheDir,
                targetStreamProvider = { syncDirUri, devId ->
                    openTargetOutputStream(context, syncDirUri, devId)
                }
            )

            exportedEntries = exportResults.filter { it.success }.sumOf { it.entriesExported }

            // Log results
            val failedImports = importResults.filter { !it.success }
            val failedExports = exportResults.filter { !it.success }
            if (failedImports.isNotEmpty()) {
                Log.w(TAG, "Failed imports: ${failedImports.map { "${it.containerName}: ${it.errorMessage}" }}")
            }
            if (failedExports.isNotEmpty()) {
                Log.w(TAG, "Failed exports: ${failedExports.map { "${it.groupName}: ${it.errorMessage}" }}")
            }

            // Update last sync time
            PreferencesUtil.setKeeShareLastSyncTime(context, System.currentTimeMillis())

            // Put results in bundle for the activity callback
            result.data = Bundle().apply {
                putInt(RESULT_IMPORTED_ENTRIES, importedEntries)
                putInt(RESULT_IMPORTED_DEVICES, importedDevices)
                putInt(RESULT_EXPORTED_ENTRIES, exportedEntries)
            }
        } catch (e: Exception) {
            Log.e(TAG, "KeeShare sync failed", e)
            setError(e)
        }

        super.onActionRun()
    }

    companion object {
        private val TAG = KeeShareSyncRunnable::class.java.simpleName

        const val RESULT_IMPORTED_ENTRIES = "KEESHARE_IMPORTED_ENTRIES"
        const val RESULT_IMPORTED_DEVICES = "KEESHARE_IMPORTED_DEVICES"
        const val RESULT_EXPORTED_ENTRIES = "KEESHARE_EXPORTED_ENTRIES"

        /**
         * Resolve the device ID for this device.
         *
         * Priority:
         * 1. User-configured device ID in preferences
         * 2. Generate and persist a fallback random ID
         */
        fun resolveDeviceId(context: Context): String {
            val configuredId = PreferencesUtil.getKeeShareDeviceId(context)
            if (!configuredId.isNullOrEmpty()) {
                return configuredId
            }

            val fallbackId = DeviceIdentity.generateFallbackDeviceId()
            PreferencesUtil.setKeeShareDeviceId(context, fallbackId)
            return fallbackId
        }

        /**
         * List other devices' container files via SAF and return open InputStreams.
         */
        fun listOtherDeviceStreams(
            context: Context,
            syncDirUri: String,
            ownDeviceId: String
        ): List<Pair<String, InputStream>> {
            val treeUri = try {
                Uri.parse(syncDirUri)
            } catch (e: Exception) {
                Log.w(TAG, "Invalid sync dir URI: $syncDirUri", e)
                return emptyList()
            }

            val dir = DocumentFile.fromTreeUri(context, treeUri)
            if (dir == null || !dir.exists()) {
                Log.d(TAG, "Sync dir not accessible: $syncDirUri")
                return emptyList()
            }

            val ownFileName = PerDeviceSyncConfig.containerFileName(ownDeviceId)
            return dir.listFiles()
                .filter { doc ->
                    doc.isFile
                        && doc.name?.endsWith(".kdbx", ignoreCase = true) == true
                        && !doc.name.equals(ownFileName, ignoreCase = true)
                }
                .sortedBy { it.name }
                .mapNotNull { doc ->
                    val stream = context.contentResolver.openInputStream(doc.uri)
                    if (stream != null) (doc.name ?: "unknown") to stream else null
                }
        }

        /**
         * Open an OutputStream to write this device's container file via SAF.
         */
        fun openTargetOutputStream(
            context: Context,
            syncDirUri: String,
            deviceId: String
        ): java.io.OutputStream? {
            val treeUri = try {
                Uri.parse(syncDirUri)
            } catch (e: Exception) {
                Log.w(TAG, "Invalid sync dir URI: $syncDirUri", e)
                return null
            }

            val dir = DocumentFile.fromTreeUri(context, treeUri)
            if (dir == null || !dir.exists()) {
                Log.w(TAG, "Sync dir not accessible for export: $syncDirUri")
                return null
            }

            val fileName = PerDeviceSyncConfig.containerFileName(deviceId)
            val existing = dir.findFile(fileName)
            val targetDoc = existing ?: dir.createFile("application/octet-stream", fileName)

            return targetDoc?.uri?.let { context.contentResolver.openOutputStream(it) }
        }
    }
}
