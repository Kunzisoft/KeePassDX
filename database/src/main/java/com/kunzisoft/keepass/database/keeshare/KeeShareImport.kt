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
package com.kunzisoft.keepass.database.keeshare

import android.util.Log
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.node.NodeHandler
import com.kunzisoft.keepass.database.merge.DatabaseKDBXMerger
import java.io.File
import java.io.InputStream

/**
 * Orchestrates KeeShare import: walks all groups in the database looking for
 * KeeShare config in custom data, opens container files, and merges their
 * content into the target groups.
 */
object KeeShareImport {

    private val TAG = KeeShareImport::class.java.simpleName

    data class ImportResult(
        val groupName: String,
        val containerName: String,
        val entriesImported: Int,
        val success: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Import all KeeShare containers for all groups in the database.
     *
     * Walks every group looking for:
     * 1. [KeeShareReference.PER_DEVICE_KEY] custom data (per-device sync, preferred)
     * 2. [KeeShareReference.CLASSIC_KEY] custom data (classic KeeShareXC fallback)
     *
     * @param database The target database to merge content into
     * @param ownDeviceId This device's short ID (to skip own container file)
     * @param cacheDirectory Directory for temporary binary storage
     * @param fileProvider Resolves a sync directory path to a list of (filename, InputStream) pairs
     * @param singleFileProvider Resolves a single file path to an InputStream (for classic KeeShare)
     * @param isRAMSufficient Callback to check RAM availability
     * @return List of import results for each container processed
     */
    fun importAll(
        database: DatabaseKDBX,
        ownDeviceId: String,
        cacheDirectory: File,
        fileProvider: (syncDir: String, ownDeviceId: String) -> List<Pair<String, InputStream>>,
        singleFileProvider: ((path: String) -> InputStream?)? = null,
        isRAMSufficient: (Long) -> Boolean = { true }
    ): List<ImportResult> {
        val results = mutableListOf<ImportResult>()
        val groupsToProcess = mutableListOf<GroupKDBX>()

        // Collect all groups with KeeShare config
        database.rootGroup?.doForEachChild(
            null,
            object : NodeHandler<GroupKDBX>() {
                override fun operate(node: GroupKDBX): Boolean {
                    val hasPerDevice = node.customData.get(KeeShareReference.PER_DEVICE_KEY) != null
                    val hasClassic = node.customData.get(KeeShareReference.CLASSIC_KEY) != null
                    if (hasPerDevice || hasClassic) {
                        groupsToProcess.add(node)
                    }
                    return true
                }
            }
        )

        // Also check root group (doForEachChild only walks children)
        database.rootGroup?.let { root ->
            val hasPerDevice = root.customData.get(KeeShareReference.PER_DEVICE_KEY) != null
            val hasClassic = root.customData.get(KeeShareReference.CLASSIC_KEY) != null
            if (hasPerDevice || hasClassic) {
                groupsToProcess.add(0, root)
            }
        }

        for (group in groupsToProcess) {
            results.addAll(importGroup(database, group, ownDeviceId, cacheDirectory,
                fileProvider, singleFileProvider, isRAMSufficient))
        }

        return results
    }

    private fun importGroup(
        database: DatabaseKDBX,
        group: GroupKDBX,
        ownDeviceId: String,
        cacheDirectory: File,
        fileProvider: (syncDir: String, ownDeviceId: String) -> List<Pair<String, InputStream>>,
        singleFileProvider: ((path: String) -> InputStream?)?,
        isRAMSufficient: (Long) -> Boolean
    ): List<ImportResult> {
        val results = mutableListOf<ImportResult>()
        val groupName = group.title

        // Priority 1: Per-device sync config
        val perDeviceData = group.customData.get(KeeShareReference.PER_DEVICE_KEY)
        if (perDeviceData != null) {
            val config = PerDeviceSyncConfig.fromCustomData(perDeviceData.value)
            if (config != null) {
                val containers = fileProvider(config.syncDir, ownDeviceId)
                for ((fileName, inputStream) in containers) {
                    results.add(
                        importContainer(database, group, groupName, fileName,
                            inputStream, config.password, cacheDirectory, isRAMSufficient)
                    )
                }
                return results
            }
        }

        // Priority 2: Classic KeeShare/Reference (fallback for KeePassXC-created databases)
        val classicData = group.customData.get(KeeShareReference.CLASSIC_KEY)
        if (classicData != null && singleFileProvider != null) {
            val ref = KeeShareReference.fromClassicCustomData(classicData.value)
            if (ref != null && ref.path.isNotEmpty()
                && (ref.type == KeeShareReference.Type.IMPORT
                    || ref.type == KeeShareReference.Type.SYNCHRONIZE)
            ) {
                val inputStream = singleFileProvider(ref.path)
                if (inputStream != null) {
                    results.add(
                        importContainer(database, group, groupName, ref.path,
                            inputStream, ref.password, cacheDirectory, isRAMSufficient)
                    )
                }
            }
        }

        return results
    }

    private fun importContainer(
        database: DatabaseKDBX,
        targetGroup: GroupKDBX,
        groupName: String,
        containerName: String,
        inputStream: InputStream,
        password: String,
        cacheDirectory: File,
        isRAMSufficient: (Long) -> Boolean
    ): ImportResult {
        return try {
            inputStream.use { stream ->
                val containerDb = KeeShareContainer.read(
                    stream, password, cacheDirectory, isRAMSufficient
                )

                val entryCount = countEntries(containerDb)

                val merger = DatabaseKDBXMerger(database).apply {
                    this.isRAMSufficient = isRAMSufficient
                }
                merger.mergeIntoGroup(containerDb, targetGroup)

                Log.i(TAG, "Imported $entryCount entries from $containerName into $groupName")
                ImportResult(groupName, containerName, entryCount, success = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import $containerName into $groupName", e)
            ImportResult(groupName, containerName, 0, success = false,
                errorMessage = e.message)
        }
    }

    private fun countEntries(database: DatabaseKDBX): Int {
        var count = 0
        database.rootGroup?.doForEachChild(
            object : NodeHandler<EntryKDBX>() {
                override fun operate(node: EntryKDBX): Boolean {
                    count++
                    return true
                }
            },
            null
        )
        return count
    }
}
