/*
 * Copyright 2022 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.merge

import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.utils.readAllBytes
import java.io.IOException

class DatabaseKDBXMerger(private var database: DatabaseKDBX) {

    var isRAMSufficient: (memoryWanted: Long) -> Boolean = {true}

    fun merge(databaseToMerge: DatabaseKDBX) {

        if (database.nameChanged.date.before(databaseToMerge.nameChanged.date)) {
            database.name = databaseToMerge.name
            database.nameChanged = databaseToMerge.nameChanged
        }
        if (database.descriptionChanged.date.before(databaseToMerge.descriptionChanged.date)) {
            database.description = databaseToMerge.description
            database.descriptionChanged = databaseToMerge.descriptionChanged
        }
        if (database.defaultUserNameChanged.date.before(databaseToMerge.defaultUserNameChanged.date)) {
            database.defaultUserName = databaseToMerge.defaultUserName
            database.defaultUserNameChanged = databaseToMerge.defaultUserNameChanged
        }
        if (database.keyLastChanged.date.before(databaseToMerge.keyLastChanged.date)) {
            database.keyChangeRecDays = databaseToMerge.keyChangeRecDays
            database.keyChangeForceDays = databaseToMerge.keyChangeForceDays
            database.isKeyChangeForceOnce = databaseToMerge.isKeyChangeForceOnce
            database.keyLastChanged = databaseToMerge.keyLastChanged
        }
        if (database.recycleBinChanged.date.before(databaseToMerge.recycleBinChanged.date)) {
            database.isRecycleBinEnabled = databaseToMerge.isRecycleBinEnabled
            database.recycleBinUUID = databaseToMerge.recycleBinUUID
            database.recycleBinChanged = databaseToMerge.recycleBinChanged
        }
        if (database.entryTemplatesGroupChanged.date.before(databaseToMerge.entryTemplatesGroupChanged.date)) {
            database.entryTemplatesGroup = databaseToMerge.entryTemplatesGroup
            database.entryTemplatesGroupChanged = databaseToMerge.entryTemplatesGroupChanged
        }
        if (database.settingsChanged.date.before(databaseToMerge.settingsChanged.date)) {
            database.color = databaseToMerge.color
            database.compressionAlgorithm = databaseToMerge.compressionAlgorithm
            database.historyMaxItems = databaseToMerge.historyMaxItems
            database.historyMaxSize = databaseToMerge.historyMaxSize
            database.encryptionAlgorithm = databaseToMerge.encryptionAlgorithm
            database.kdfParameters = databaseToMerge.kdfParameters
            database.numberKeyEncryptionRounds = databaseToMerge.numberKeyEncryptionRounds
            database.memoryUsage = databaseToMerge.memoryUsage
            database.parallelism = databaseToMerge.parallelism
            database.settingsChanged = databaseToMerge.settingsChanged
        }

        val databaseRootGroupId = database.rootGroup?.nodeId
        val databaseRootGroupIdToMerge = databaseToMerge.rootGroup?.nodeId

        if (databaseRootGroupId == null || databaseRootGroupIdToMerge == null) {
            throw IOException("Database is not open")
        }

        // UUID of the root group to merge is unknown
        if (database.getGroupById(databaseRootGroupIdToMerge) == null) {
            // Change it to copy children database root
            // TODO Test merge root
            databaseToMerge.rootGroup?.let { databaseRootGroupToMerge ->
                databaseToMerge.removeGroupIndex(databaseRootGroupToMerge)
                databaseRootGroupToMerge.nodeId = databaseRootGroupId
                databaseToMerge.updateGroup(databaseRootGroupToMerge)
            }
        }

        databaseToMerge.rootGroup?.doForEachChild(
            object : NodeHandler<EntryKDBX>() {
                override fun operate(node: EntryKDBX): Boolean {
                    mergeEntry(node, databaseToMerge)
                    return true
                }
            },
            object : NodeHandler<GroupKDBX>() {
                override fun operate(node: GroupKDBX): Boolean {
                    mergeGroup(node, databaseToMerge)
                    return true
                }
            }
        )

        // TODO merge custom data

        databaseToMerge.iconsManager.doForEachCustomIcon { iconImageCustom, binaryData ->
            val customIconUuid = iconImageCustom.uuid
            // If custom icon not present, add it
            val customIcon = database.iconsManager.getIcon(customIconUuid)
            if (customIcon == null) {
                database.addCustomIcon(
                    customIconUuid,
                    iconImageCustom.name,
                    iconImageCustom.lastModificationTime,
                    false
                ) { _, newBinaryData ->
                    binaryData.getInputDataStream(databaseToMerge.binaryCache).use { inputStream ->
                        newBinaryData?.getOutputDataStream(database.binaryCache).use { outputStream ->
                            inputStream.readAllBytes { buffer ->
                                outputStream?.write(buffer)
                            }
                        }
                    }
                }
            } else {
                val customIconModification = customIcon.lastModificationTime
                val customIconToMerge = databaseToMerge.iconsManager.getIcon(customIconUuid)
                val customIconModificationToMerge = customIconToMerge?.lastModificationTime
                if (customIconModification != null && customIconModificationToMerge != null) {
                    if (customIconModification.date.before(customIconModificationToMerge.date)) {
                        customIcon.updateWith(customIconToMerge)
                    }
                } else if (customIconModificationToMerge != null) {
                    customIcon.updateWith(customIconToMerge)
                }
            }
        }

        databaseToMerge.deletedObjects.forEach { deletedObject ->
            val deletedObjectId = deletedObject.uuid
            val databaseEntry = database.getEntryById(deletedObjectId)
            val databaseGroup = database.getGroupById(deletedObjectId)
            val databaseIcon = database.iconsManager.getIcon(deletedObjectId)
            val databaseIconModificationTime = databaseIcon?.lastModificationTime
            if (databaseEntry != null
                && deletedObject.deletionTime.date
                    .after(databaseEntry.lastModificationTime.date)) {
                database.removeEntryFrom(databaseEntry, databaseEntry.parent)
            }
            if (databaseGroup != null
                && deletedObject.deletionTime.date
                    .after(databaseGroup.lastModificationTime.date)) {
                database.removeGroupFrom(databaseGroup, databaseGroup.parent)
            }
            if (databaseIcon != null
                && (
                    databaseIconModificationTime == null
                    || (deletedObject.deletionTime.date.after(databaseIconModificationTime.date))
                    )
            ) {
                database.removeCustomIcon(deletedObjectId)
            }
            // Attachments are removed and optimized during the database save
        }
    }

    private fun mergeEntry(nodeToMerge: EntryKDBX, databaseToMerge: DatabaseKDBX) {
        val entryId = nodeToMerge.nodeId
        val entryToMerge = databaseToMerge.getEntryById(entryId)
        val entry = database.getEntryById(entryId)
        val deletedObject = database.getDeletedObject(entryId)

        if (entryToMerge != null) {
            // Retrieve parent in current database
            var parentEntryToMerge: GroupKDBX? = null
            entryToMerge.parent?.nodeId?.let {
                parentEntryToMerge = database.getGroupById(it)
            }
            // Copy attachments in main pool
            val newAttachments = mutableListOf<Attachment>()
            entryToMerge.getAttachments(databaseToMerge.attachmentPool).forEach { attachment ->
                val binarySize = attachment.binaryData.getSize()
                val binaryData = database.buildNewBinaryAttachment(
                    isRAMSufficient.invoke(binarySize),
                    attachment.binaryData.isCompressed,
                    attachment.binaryData.isProtected
                )
                attachment.binaryData.getInputDataStream(databaseToMerge.binaryCache).use { inputStream ->
                    binaryData.getOutputDataStream(database.binaryCache).use { outputStream ->
                        inputStream.readAllBytes { buffer ->
                            outputStream.write(buffer)
                        }
                    }
                }
                newAttachments.add(Attachment(attachment.name, binaryData))
            }
            entryToMerge.removeAttachments()
            newAttachments.forEach { newAttachment ->
                entryToMerge.putAttachment(newAttachment, database.attachmentPool)
            }

            if (entry == null) {
                // If it's a deleted object, but another instance was updated
                // If entry parent to add exists and in current database
                if ((deletedObject == null
                    || deletedObject.deletionTime.date
                        .before(entryToMerge.lastModificationTime.date))
                    && parentEntryToMerge != null) {
                    database.addEntryTo(entryToMerge, parentEntryToMerge)
                }
            } else if (entry.lastModificationTime.date
                    .before(entryToMerge.lastModificationTime.date)
            ) {
                // TODO custom Data
                // Keep entry as history if already not present
                entry.history.forEach { history ->
                    // If history not present
                    if (!entryToMerge.history.any {
                            it.lastModificationTime == history.lastModificationTime
                    }) {
                        entryToMerge.addEntryToHistory(history)
                    }
                }
                // Last entry not present
                val history = EntryKDBX().apply {
                    updateWith(entry, false)
                    parent = null
                }
                entryToMerge.addEntryToHistory(history)
                if (parentEntryToMerge == entry.parent) {
                    entry.updateWith(entryToMerge)
                } else {
                    // Update entry with databaseEntryToMerge and merge history
                    database.removeEntryFrom(entry, entry.parent)
                    if (parentEntryToMerge != null) {
                        database.addEntryTo(entryToMerge, parentEntryToMerge)
                    }
                }
            } else {
                // TODO custom Data
                entryToMerge.history.forEach { history ->
                    if (!entry.history.any {
                            it.lastModificationTime == history.lastModificationTime
                        }) {
                        entry.addEntryToHistory(history)
                    }
                }
                // Keep entry to merge as history
                val history = EntryKDBX().apply {
                    updateWith(entryToMerge, false)
                    parent = null
                }
                entry.addEntryToHistory(history)
            }
        }
    }

    private fun mergeGroup(nodeToMerge: GroupKDBX, databaseToMerge: DatabaseKDBX) {
        val groupId = nodeToMerge.nodeId
        val groupToMerge = databaseToMerge.getGroupById(groupId)
        val group = database.getGroupById(groupId)
        val deletedObject = database.getDeletedObject(groupId)

        if (groupToMerge != null) {
            // Retrieve parent in current database
            var parentGroupToMerge: GroupKDBX? = null
            groupToMerge.parent?.nodeId?.let {
                parentGroupToMerge = database.getGroupById(it)
            }

            if (group == null) {
                // If group parent to add exists and in current database
                if ((deletedObject == null
                    || deletedObject.deletionTime.date
                        .before(groupToMerge.lastModificationTime.date))
                    && parentGroupToMerge != null) {
                    database.addGroupTo(groupToMerge, parentGroupToMerge)
                }
            } else if (group.lastModificationTime.date
                    .before(groupToMerge.lastModificationTime.date)
            ) {
                // TODO custom Data
                if (parentGroupToMerge == group.parent) {
                    group.updateWith(groupToMerge)
                } else {
                    database.removeGroupFrom(group, group.parent)
                    if (parentGroupToMerge != null) {
                        database.addGroupTo(groupToMerge, parentGroupToMerge)
                    }
                }
            } else {
                // TODO custom Data
            }
        }
    }
}