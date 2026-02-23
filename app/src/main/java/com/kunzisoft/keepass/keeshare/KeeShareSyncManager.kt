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
package com.kunzisoft.keepass.keeshare

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.action.KeeShareSyncRunnable
import com.kunzisoft.keepass.database.keeshare.KeeShareExport
import com.kunzisoft.keepass.database.keeshare.KeeShareReference
import com.kunzisoft.keepass.database.keeshare.PerDeviceSyncConfig
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages KeeShare auto-sync lifecycle: periodic polling for newer container
 * files, export-on-save, and stale file cleanup.
 *
 * Extracted from DatabaseTaskNotificationService to keep sync orchestration
 * separate from the global notification service.
 */
class KeeShareSyncManager(
    private val context: Context,
    private val mainScope: CoroutineScope,
    private val isActionRunning: () -> Boolean,
    private val startSyncService: () -> Unit
) {

    private var periodicJob: Job? = null

    fun startAutoSync(database: ContextualDatabase) {
        stopAutoSync()

        val kdbx = database.databaseKDBX ?: return
        if (database.isReadOnly) return

        // Collect sync directory URIs from groups with KeeShare config
        val syncDirUris = mutableSetOf<String>()
        val groupHandler = object : com.kunzisoft.keepass.database.element.node.NodeHandler<com.kunzisoft.keepass.database.element.group.GroupKDBX>() {
            override fun operate(node: com.kunzisoft.keepass.database.element.group.GroupKDBX): Boolean {
                val perDeviceData = node.customData.get(KeeShareReference.PER_DEVICE_KEY)
                if (perDeviceData != null) {
                    val config = PerDeviceSyncConfig.fromCustomData(perDeviceData.value)
                    if (config != null) {
                        syncDirUris.add(config.syncDir)
                    }
                }
                return true
            }
        }
        kdbx.rootGroup?.doForEachChild(null, groupHandler)
        kdbx.rootGroup?.let { groupHandler.operate(it) }

        if (syncDirUris.isEmpty()) return

        // Start periodic sync timer
        periodicJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(PERIODIC_SYNC_INTERVAL_MS)
                if (!isActive) break
                val lastSyncTime = PreferencesUtil.getKeeShareLastSyncTime(context)
                if (hasNewerContainerFiles(context, syncDirUris, lastSyncTime)) {
                    withContext(Dispatchers.Main) {
                        if (!isActionRunning()) {
                            startSyncService()
                        }
                    }
                }

                // Cleanup stale device files via SAF
                val staleDays = PreferencesUtil.getKeeShareStaleDays(context)
                if (staleDays > 0) {
                    val deviceId = KeeShareSyncRunnable.resolveDeviceId(context)
                    for (syncDirUri in syncDirUris) {
                        cleanupStaleDeviceFiles(context, syncDirUri, deviceId, staleDays)
                    }
                }
            }
        }

        Log.i(TAG, "KeeShare auto-sync started for ${syncDirUris.size} directories")
    }

    fun stopAutoSync() {
        periodicJob?.cancel()
        periodicJob = null
    }

    /**
     * Lightweight export-only: writes container files for all KeeShare-configured
     * groups without importing or re-saving the database. Runs on IO thread.
     */
    fun triggerExportAfterSave(database: ContextualDatabase) {
        val kdbx = database.databaseKDBX ?: return
        if (database.isReadOnly) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceId = KeeShareSyncRunnable.resolveDeviceId(context)
                val cacheDir = File(context.cacheDir, "keeshare")
                cacheDir.mkdirs()

                val results = KeeShareExport.exportAll(
                    database = kdbx,
                    deviceId = deviceId,
                    cacheDirectory = cacheDir,
                    targetStreamProvider = { syncDirUri, devId ->
                        KeeShareSyncRunnable.openTargetOutputStream(context, syncDirUri, devId)
                    }
                )

                val exported = results.filter { it.success }
                if (exported.isNotEmpty()) {
                    PreferencesUtil.setKeeShareLastSyncTime(
                        context, System.currentTimeMillis()
                    )
                    Log.i(TAG, "KeeShare export-on-save: exported " +
                        "${exported.sumOf { it.entriesExported }} entries to " +
                        "${exported.size} containers")
                }
            } catch (e: Exception) {
                Log.w(TAG, "KeeShare export-on-save failed", e)
            }
        }
    }

    companion object {
        private val TAG = KeeShareSyncManager::class.java.simpleName

        private const val PERIODIC_SYNC_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

        /**
         * Check if any container files in the sync directories have been
         * modified since [lastSyncTime] using SAF DocumentFile metadata.
         */
        fun hasNewerContainerFiles(
            context: Context,
            syncDirUris: Set<String>,
            lastSyncTime: Long
        ): Boolean {
            for (syncDirUri in syncDirUris) {
                val treeUri = try {
                    Uri.parse(syncDirUri)
                } catch (e: Exception) {
                    continue
                }

                val dir = DocumentFile.fromTreeUri(context, treeUri) ?: continue
                if (!dir.exists()) continue

                for (doc in dir.listFiles()) {
                    if (doc.isFile
                        && doc.name?.endsWith(".kdbx", ignoreCase = true) == true
                        && doc.lastModified() > lastSyncTime
                    ) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * Remove container files from other devices that haven't been modified
         * within [maxAgeDays] days via SAF. Never removes the own device's file.
         */
        fun cleanupStaleDeviceFiles(
            context: Context,
            syncDirUri: String,
            ownDeviceId: String,
            maxAgeDays: Int
        ) {
            if (maxAgeDays <= 0) return

            val treeUri = try {
                Uri.parse(syncDirUri)
            } catch (e: Exception) {
                return
            }

            val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return
            if (!dir.exists()) return

            val ownFileName = PerDeviceSyncConfig.containerFileName(ownDeviceId)
            val cutoffTime = System.currentTimeMillis() - (maxAgeDays.toLong() * 24 * 60 * 60 * 1000)

            for (doc in dir.listFiles()) {
                if (!doc.isFile) continue
                val name = doc.name ?: continue
                if (!name.endsWith(".kdbx", ignoreCase = true)) continue
                if (name.equals(ownFileName, ignoreCase = true)) continue

                if (doc.lastModified() < cutoffTime) {
                    if (doc.delete()) {
                        Log.i(TAG, "Cleaned up stale device file: $name")
                    }
                }
            }
        }
    }
}
