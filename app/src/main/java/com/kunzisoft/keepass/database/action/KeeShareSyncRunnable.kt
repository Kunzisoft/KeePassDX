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
import com.kunzisoft.keepass.database.element.CustomDataItem
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.keeshare.DeviceIdentity
import com.kunzisoft.keepass.database.keeshare.KeeShareExport
import com.kunzisoft.keepass.database.keeshare.KeeShareImport
import com.kunzisoft.keepass.database.keeshare.KeeShareReference
import com.kunzisoft.keepass.database.keeshare.PerDeviceSyncConfig
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import java.io.InputStream
import java.io.OutputStream

class KeeShareSyncRunnable(
    context: Context,
    database: ContextualDatabase,
    saveDatabase: Boolean,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    private val progressTaskUpdater: ProgressTaskUpdater?,
    private val silentSync: Boolean = false,
    private val importOnly: Boolean = false
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
        if (!silentSync) {
            database.wasReloaded = true
        }
        super.onStartRun()
    }

    override fun onActionRun() {
        Log.d(TAG, "=== KeeShare sync onActionRun() START ===")
        try {
            val deviceId = resolveDeviceId(context)
            Log.d(TAG, "Device ID: $deviceId")

            // Auto-provision PerDeviceSync from preferences sync folder + classic reference
            val prefSyncFolder = PreferencesUtil.getKeeShareSyncFolderUri(context)
            Log.d(TAG, "Preferences sync folder URI: ${prefSyncFolder ?: "(not set)"}")
            if (!prefSyncFolder.isNullOrEmpty()) {
                provisionPerDeviceConfig(database, prefSyncFolder)
            } else {
                Log.w(TAG, "No sync folder configured in preferences — auto-provisioning skipped")
            }

            // 1. Import from all other device containers via SAF
            Log.d(TAG, "--- Starting import phase ---")
            val importResults = KeeShareImport.importAll(
                database = database,
                ownDeviceId = deviceId,
                fileProvider = { syncDirUri, ownId ->
                    listOtherDeviceStreams(context, syncDirUri, ownId) { fileName ->
                        PreferencesUtil.getKeeShareFileMtime(context, fileName)
                    }
                },
                isRAMSufficient = { memoryWanted ->
                    BinaryData.canMemoryBeAllocatedInRAM(context, memoryWanted)
                }
            )

            // Store per-file mtimes for successful imports
            for (r in importResults) {
                if (r.success && r.fileMtime > 0) {
                    PreferencesUtil.setKeeShareFileMtime(context, r.containerName, r.fileMtime)
                }
            }

            importedEntries = importResults.filter { it.success }.sumOf { it.entriesImported }
            importedDevices = importResults.filter { it.success }
                .map { it.containerName }.distinct().size
            Log.d(TAG, "Import results: ${importResults.size} total, $importedEntries entries from $importedDevices devices")
            for (r in importResults) {
                Log.d(TAG, "  Import: group='${r.groupName}' container='${r.containerName}' entries=${r.entriesImported} success=${r.success} error=${r.errorMessage}")
            }

            if (silentSync && importedEntries > 0) {
                database.wasReloaded = true
            }

            // 2. Export own container for each shared group via SAF
            // Skip export when triggered by auto-sync (importOnly) to prevent
            // feedback loops between devices via Syncthing
            if (!importOnly) {
                Log.d(TAG, "--- Starting export phase ---")
                val exportResults = KeeShareExport.exportAll(
                    database = database,
                    deviceId = deviceId,
                    targetStreamProvider = { syncDirUri, devId ->
                        openTargetOutputStream(context, syncDirUri, devId)
                    }
                )

                exportedEntries = exportResults.filter { it.success }.sumOf { it.entriesExported }
                Log.d(TAG, "Export results: ${exportResults.size} total, $exportedEntries entries exported")
                for (r in exportResults) {
                    Log.d(TAG, "  Export: group='${r.groupName}' entries=${r.entriesExported} success=${r.success} error=${r.errorMessage}")
                }

                val failedExports = exportResults.filter { !it.success }
                if (failedExports.isNotEmpty()) {
                    Log.w(TAG, "Failed exports: ${failedExports.map { "${it.groupName}: ${it.errorMessage}" }}")
                }
            } else {
                Log.d(TAG, "--- Export phase SKIPPED (importOnly=true) ---")
            }

            // Log failed imports
            val failedImports = importResults.filter { !it.success }
            if (failedImports.isNotEmpty()) {
                Log.w(TAG, "Failed imports: ${failedImports.map { "${it.containerName}: ${it.errorMessage}" }}")
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
         * For groups that have a classic KeeShare/Reference (from KeePassXC) with
         * per-device mode but no KeeShare/PerDeviceSync custom data, auto-create
         * the PerDeviceSync config using the sync folder URI from preferences.
         */
        private fun provisionPerDeviceConfig(
            database: Database,
            syncFolderUri: String
        ) {
            val allGroups = mutableListOf<Group>()
            database.rootGroup?.let { allGroups.add(it) }
            allGroups.addAll(database.getAllGroupsWithoutRoot())

            for (group in allGroups) {
                // Skip if already has PerDeviceSync
                if (group.customData.get(KeeShareReference.PER_DEVICE_KEY) != null) continue

                val classicData = group.customData.get(KeeShareReference.CLASSIC_KEY) ?: continue
                val ref = KeeShareReference.fromClassicCustomData(classicData.value) ?: continue

                if (!ref.isPerDeviceMode()) continue
                if (ref.type != KeeShareReference.Type.SYNCHRONIZE
                    && ref.type != KeeShareReference.Type.IMPORT) continue

                val config = PerDeviceSyncConfig(
                    syncDir = syncFolderUri,
                    password = ref.password,
                    keepGroups = ref.keepGroups
                )
                val encoded = PerDeviceSyncConfig.toCustomData(config)
                group.customData.put(
                    CustomDataItem(
                        KeeShareReference.PER_DEVICE_KEY,
                        encoded,
                        DateInstant()
                    )
                )
                Log.i(TAG, "Auto-provisioned PerDeviceSync for group '${group.title}' with folder: $syncFolderUri")
            }
        }

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
         * Skips files whose mtime hasn't changed since the last successful import.
         */
        fun listOtherDeviceStreams(
            context: Context,
            syncDirUri: String,
            ownDeviceId: String,
            fileMtimeProvider: (String) -> Long = { 0L }
        ): List<Triple<String, InputStream, Long>> {
            Log.d(TAG, "listOtherDeviceStreams: syncDirUri=$syncDirUri, ownDeviceId=$ownDeviceId")
            val treeUri = try {
                Uri.parse(syncDirUri)
            } catch (e: Exception) {
                Log.w(TAG, "Invalid sync dir URI: $syncDirUri", e)
                return emptyList()
            }
            Log.d(TAG, "  Parsed URI: $treeUri (scheme=${treeUri.scheme})")

            val dir = DocumentFile.fromTreeUri(context, treeUri)
            Log.d(TAG, "  DocumentFile: exists=${dir?.exists()}, isDir=${dir?.isDirectory}, name=${dir?.name}")
            if (dir == null || !dir.exists()) {
                Log.d(TAG, "Sync dir not accessible: $syncDirUri")
                return emptyList()
            }

            val allFiles = dir.listFiles()
            Log.d(TAG, "  Directory contains ${allFiles.size} files: ${allFiles.map { it.name }}")
            val ownFileName = PerDeviceSyncConfig.containerFileName(ownDeviceId)
            Log.d(TAG, "  Own filename to skip: $ownFileName")
            return allFiles
                .filter { doc ->
                    doc.isFile
                        && doc.name?.endsWith(".kdbx", ignoreCase = true) == true
                        && !doc.name.equals(ownFileName, ignoreCase = true)
                }
                .sortedBy { it.name }
                .mapNotNull { doc ->
                    val name = doc.name ?: "unknown"
                    val mtime = doc.lastModified()
                    val storedMtime = fileMtimeProvider(name)
                    if (mtime > 0 && mtime <= storedMtime) {
                        Log.i(TAG, "Skipping unchanged file: $name (mtime=$mtime)")
                        return@mapNotNull null
                    }
                    val stream = context.contentResolver.openInputStream(doc.uri)
                    if (stream != null) Triple(name, stream, mtime) else null
                }
        }

        /**
         * Open an OutputStream to write this device's container file via SAF.
         */
        fun openTargetOutputStream(
            context: Context,
            syncDirUri: String,
            deviceId: String
        ): OutputStream? {
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
