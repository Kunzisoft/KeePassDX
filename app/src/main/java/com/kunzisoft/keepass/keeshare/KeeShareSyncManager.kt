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
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.keeshare.KeeShareReference
import com.kunzisoft.keepass.database.keeshare.PerDeviceSyncConfig
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages KeeShare auto-sync lifecycle: ContentObserver-based detection of
 * newer container files, sync-on-open catch-up, and export-on-save.
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

    private var contentObservers: List<ContentObserver> = emptyList()
    private var debounceJob: Job? = null

    fun startAutoSync(database: ContextualDatabase) {
        stopAutoSync()

        if (database.isReadOnly) return

        val syncDirUris = database.collectSyncDirUris()

        // Also check preferences sync folder (may not yet be provisioned into groups)
        val prefSyncFolder = PreferencesUtil.getKeeShareSyncFolderUri(context)
        if (!prefSyncFolder.isNullOrEmpty()) {
            syncDirUris.add(prefSyncFolder)
        }

        if (syncDirUris.isEmpty()) return

        // Register ContentObservers on SAF tree URIs for near-real-time detection
        val observers = mutableListOf<ContentObserver>()
        val handler = Handler(Looper.getMainLooper())
        for (syncDirUri in syncDirUris) {
            val treeUri = try {
                Uri.parse(syncDirUri)
            } catch (e: Exception) {
                continue
            }
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    onChange(selfChange, null)
                }
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    if (selfChange) return
                    Log.d(TAG, "ContentObserver: change detected in $syncDirUri (uri=$uri)")
                    onSyncDirChanged()
                }
            }
            try {
                context.contentResolver.registerContentObserver(treeUri, true, observer)
                observers.add(observer)
                Log.d(TAG, "Registered ContentObserver on: $syncDirUri")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register ContentObserver on $syncDirUri", e)
            }
        }
        contentObservers = observers

        Log.i(TAG, "KeeShare auto-sync started: ${syncDirUris.size} directories, ${observers.size} observers")

        // Immediate catch-up on database open
        mainScope.launch {
            val lastSyncTime = PreferencesUtil.getKeeShareLastSyncTime(context)
            val elapsed = System.currentTimeMillis() - lastSyncTime
            if (elapsed >= MIN_SYNC_INTERVAL_MS && !isActionRunning()) {
                Log.i(TAG, "Sync-on-open: triggering immediate import")
                startSyncService()
            }
        }
    }

    /**
     * Called by ContentObserver when a change is detected in a sync directory.
     * Debounces rapid changes (e.g. Syncthing writing in chunks).
     */
    private fun onSyncDirChanged() {
        debounceJob?.cancel()
        debounceJob = mainScope.launch {
            delay(DEBOUNCE_MS)
            val lastSyncTime = PreferencesUtil.getKeeShareLastSyncTime(context)
            val elapsed = System.currentTimeMillis() - lastSyncTime
            if (elapsed < MIN_SYNC_INTERVAL_MS) {
                Log.d(TAG, "ContentObserver: skipping sync, only ${elapsed}ms since last sync")
                return@launch
            }
            if (!isActionRunning()) {
                Log.i(TAG, "ContentObserver triggered sync")
                startSyncService()
            }
        }
    }

    /**
     * Trigger export of KeeShare containers after a database save.
     * Uses the same sync service but only runs if not already syncing.
     */
    fun triggerExportAfterSave(database: ContextualDatabase) {
        if (database.isReadOnly) return
        if (isActionRunning()) return

        val syncDirUris = database.collectSyncDirUris()
        if (syncDirUris.isEmpty()) return

        Log.i(TAG, "Export-on-save: triggering sync service")
        startSyncService()
    }

    fun stopAutoSync() {
        debounceJob?.cancel()
        debounceJob = null
        for (observer in contentObservers) {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister ContentObserver", e)
            }
        }
        contentObservers = emptyList()
    }

    companion object {
        private val TAG = KeeShareSyncManager::class.java.simpleName

        private const val DEBOUNCE_MS = 3000L
        private const val MIN_SYNC_INTERVAL_MS = 5000L
    }
}

/**
 * Collect sync directory URIs from groups with KeeShare per-device config.
 * Uses the abstract Database API without exposing KDBX internals.
 */
private fun ContextualDatabase.collectSyncDirUris(): MutableSet<String> {
    val uris = mutableSetOf<String>()
    forEachGroupWithCustomData(KeeShareReference.PER_DEVICE_KEY) { value ->
        val config = PerDeviceSyncConfig.fromCustomData(value)
        if (config != null) {
            uris.add(config.syncDir)
        }
    }
    return uris
}
