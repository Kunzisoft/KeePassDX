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

import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.CustomData
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.database.DatabaseKDB
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDB
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.node.NodeHandler
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdInt
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.utils.readAllBytes
import java.io.IOException
import java.util.*

class DatabaseKDBXMerger(private var database: DatabaseKDBX) {

    var isRAMSufficient: (memoryWanted: Long) -> Boolean = {true}

    /**
     * Merge a KDB database in a KDBX database, by default all data are copied from the KDB
     */
    fun merge(databaseToMerge: DatabaseKDB) {
        val rootGroup = database.rootGroup
        val rootGroupId = rootGroup?.nodeId
        val rootGroupToMerge = databaseToMerge.rootGroup
        val rootGroupIdToMerge = rootGroupToMerge?.nodeId

        if (rootGroupId == null || rootGroupIdToMerge == null) {
            throw IOException("Database is not open")
        }

        // Replace the UUID of the KDB root group to init seed
        databaseToMerge.removeGroupIndex(rootGroupToMerge)
        rootGroupToMerge.nodeId = NodeIdInt(0)
        databaseToMerge.addGroupIndex(rootGroupToMerge)

        // Merge children
        rootGroupToMerge.doForEachChild(
            object : NodeHandler<EntryKDB>() {
                override fun operate(node: EntryKDB): Boolean {
                    mergeEntry(rootGroup.nodeId, node, databaseToMerge)
                    return true
                }
            },
            object : NodeHandler<GroupKDB>() {
                override fun operate(node: GroupKDB): Boolean {
                    mergeGroup(rootGroup.nodeId, node, databaseToMerge)
                    return true
                }
            }
        )
    }

    /**
     * Utility method to transform KDB id nodes in KDBX id nodes
     */
    private fun getNodeIdUUIDFrom(seed: NodeId<UUID>, intId: NodeId<Int>): NodeId<UUID> {
        val seedUUID = seed.id
        val idInt = intId.id
        return NodeIdUUID(UUID(seedUUID.mostSignificantBits, seedUUID.leastSignificantBits + idInt))
    }

    /**
     * Utility method to merge a KDB entry
     */
    private fun mergeEntry(seed: NodeId<UUID>, nodeToMerge: EntryKDB, databaseToMerge: DatabaseKDB) {
        val entryId: NodeId<UUID> = nodeToMerge.nodeId
        val entry = database.getEntryById(entryId)

        databaseToMerge.getEntryById(entryId)?.let { srcEntryToMerge ->
            // Do not merge meta stream elements
            if (!srcEntryToMerge.isMetaStream()) {
                // Retrieve parent in current database
                var parentEntryToMerge: GroupKDBX? = null
                srcEntryToMerge.parent?.nodeId?.let {
                    val parentGroupIdToMerge = getNodeIdUUIDFrom(seed, it)
                    parentEntryToMerge = database.getGroupById(parentGroupIdToMerge)
                }
                // Copy attachment
                var newAttachment: Attachment? = null
                srcEntryToMerge.getAttachment(databaseToMerge.attachmentPool)?.let { attachment ->
                    val binarySize = attachment.binaryData.getSize()
                    val binaryData = database.buildNewBinaryAttachment(
                        isRAMSufficient.invoke(binarySize),
                        attachment.binaryData.isCompressed,
                        attachment.binaryData.isProtected
                    )
                    attachment.binaryData.getInputDataStream(databaseToMerge.binaryCache)
                        .use { inputStream ->
                            binaryData.getOutputDataStream(database.binaryCache)
                                .use { outputStream ->
                                    inputStream.readAllBytes { buffer ->
                                        outputStream.write(buffer)
                                    }
                                }
                        }
                    newAttachment = Attachment(attachment.name, binaryData)
                }
                // Create new entry format
                val entryToMerge = EntryKDBX().apply {
                    this.nodeId = srcEntryToMerge.nodeId
                    this.icon = srcEntryToMerge.icon
                    this.creationTime = DateInstant(srcEntryToMerge.creationTime)
                    this.lastModificationTime = DateInstant(srcEntryToMerge.lastModificationTime)
                    this.lastAccessTime = DateInstant(srcEntryToMerge.lastAccessTime)
                    this.expiryTime = DateInstant(srcEntryToMerge.expiryTime)
                    this.expires = srcEntryToMerge.expires
                    this.title = srcEntryToMerge.title
                    this.username = srcEntryToMerge.username
                    this.password = srcEntryToMerge.password
                    this.url = srcEntryToMerge.url
                    this.notes = srcEntryToMerge.notes
                    newAttachment?.let {
                        this.putAttachment(it, database.attachmentPool)
                    }
                }
                if (entry != null) {
                    entry.updateWith(entryToMerge, false)
                } else if (parentEntryToMerge != null) {
                    database.addEntryTo(entryToMerge, parentEntryToMerge)
                }
            }
        }
    }

    /**
     * Utility method to merge a KDB group
     */
    private fun mergeGroup(seed: NodeId<UUID>, nodeToMerge: GroupKDB, databaseToMerge: DatabaseKDB) {
        val groupId: NodeId<Int> = nodeToMerge.nodeId
        val group = database.getGroupById(getNodeIdUUIDFrom(seed, groupId))

        databaseToMerge.getGroupById(groupId)?.let { srcGroupToMerge ->
            // Retrieve parent in current database
            var parentGroupToMerge: GroupKDBX? = null
            srcGroupToMerge.parent?.nodeId?.let {
                val parentGroupIdToMerge = getNodeIdUUIDFrom(seed, it)
                parentGroupToMerge = database.getGroupById(parentGroupIdToMerge)
            }
            val groupToMerge = GroupKDBX().apply {
                this.nodeId = getNodeIdUUIDFrom(seed, srcGroupToMerge.nodeId)
                this.icon = srcGroupToMerge.icon
                this.creationTime = DateInstant(srcGroupToMerge.creationTime)
                this.lastModificationTime = DateInstant(srcGroupToMerge.lastModificationTime)
                this.lastAccessTime = DateInstant(srcGroupToMerge.lastAccessTime)
                this.expiryTime = DateInstant(srcGroupToMerge.expiryTime)
                this.expires = srcGroupToMerge.expires
                this.title = srcGroupToMerge.title
            }
            if (group != null) {
                group.updateWith(groupToMerge, false)
            } else if (parentGroupToMerge != null) {
                database.addGroupTo(groupToMerge, parentGroupToMerge)
            }
        }
    }

    /**
     * Merge a KDB> database in a KDBX database,
     * Try to take into account the modification date of each element
     * To make a merge as accurate as possible
     */
    fun merge(databaseToMerge: DatabaseKDBX) {

        // Merge settings
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
            database.kdfEngine = databaseToMerge.kdfEngine
            database.numberKeyEncryptionRounds = databaseToMerge.numberKeyEncryptionRounds
            database.memoryUsage = databaseToMerge.memoryUsage
            database.parallelism = databaseToMerge.parallelism
            database.settingsChanged = databaseToMerge.settingsChanged
        }

        val rootGroup = database.rootGroup
        val rootGroupId = rootGroup?.nodeId
        val rootGroupToMerge = databaseToMerge.rootGroup
        val rootGroupIdToMerge = rootGroupToMerge?.nodeId

        if (rootGroupId == null || rootGroupIdToMerge == null) {
            throw IOException("Database is not open")
        }

        // UUID of the root group to merge is unknown
        if (database.getGroupById(rootGroupIdToMerge) == null) {
            // Change it to copy children database root
            databaseToMerge.removeGroupIndex(rootGroupToMerge)
            rootGroupToMerge.nodeId = rootGroupId
            databaseToMerge.addGroupIndex(rootGroupToMerge)
        }

        // Merge root group
        if (rootGroup.lastModificationTime.date
                .before(rootGroupToMerge.lastModificationTime.date)) {
            rootGroup.updateWith(rootGroupToMerge, updateParents = false)
        }
        // Merge children
        rootGroupToMerge.doForEachChild(
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

        // Merge custom data in database header
        mergeCustomData(database.customData, databaseToMerge.customData)

        // Merge icons
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

        // Manage deleted objects
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

    /**
     * Merge [customDataToMerge] in [customData]
     */
    private fun mergeCustomData(customData: CustomData, customDataToMerge: CustomData) {
        customDataToMerge.doForEachItems { customDataItemToMerge ->
            val customDataItem = customData.get(customDataItemToMerge.key)
            if (customDataItem == null) {
                customData.put(customDataItemToMerge)
            } else {
                val customDataItemModification = customDataItem.lastModificationTime
                val customDataItemToMergeModification = customDataItemToMerge.lastModificationTime
                if (customDataItemModification != null && customDataItemToMergeModification != null) {
                    if (customDataItemModification.date
                            .before(customDataItemToMergeModification.date)) {
                        customData.put(customDataItemToMerge)
                    }
                } else {
                    customData.put(customDataItemToMerge)
                }
            }
        }
    }

    /**
     * Utility method to merge a KDBX entry
     */
    private fun mergeEntry(nodeToMerge: EntryKDBX, databaseToMerge: DatabaseKDBX) {
        val entryId = nodeToMerge.nodeId
        val entry = database.getEntryById(entryId)
        val deletedObject = database.getDeletedObject(entryId)

        databaseToMerge.getEntryById(entryId)?.let { srcEntryToMerge ->
            // Retrieve parent in current database
            var parentEntryToMerge: GroupKDBX? = null
            srcEntryToMerge.parent?.nodeId?.let {
                parentEntryToMerge = database.getGroupById(it)
            }
            val entryToMerge = EntryKDBX().apply {
                updateWith(srcEntryToMerge, copyHistory = true, updateParents = false)
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
            } else {
                // Merge independently custom data
                mergeCustomData(entry.customData, entryToMerge.customData)
                // Merge by modification time
                if (entry.lastModificationTime.date
                        .before(entryToMerge.lastModificationTime.date)
                ) {
                    addHistory(entry, entryToMerge)
                    if (parentEntryToMerge == entry.parent) {
                        entry.updateWith(entryToMerge, copyHistory = true, updateParents = false)
                    } else {
                        // Update entry with databaseEntryToMerge and merge history
                        database.removeEntryFrom(entry, entry.parent)
                        if (parentEntryToMerge != null) {
                            database.addEntryTo(entryToMerge, parentEntryToMerge)
                        }
                    }
                } else if (entry.lastModificationTime.date
                        .after(entryToMerge.lastModificationTime.date)
                ) {
                    addHistory(entryToMerge, entry)
                }
            }
        }
    }

    /**
     * Utility method to merge an history from an [entryA] to an [entryB],
     * [entryB] is modified
     */
    private fun addHistory(entryA: EntryKDBX, entryB: EntryKDBX) {
        // Keep entry as history if already not present
        entryA.history.forEach { history ->
            // If history not present
            if (!entryB.history.any {
                    it.lastModificationTime == history.lastModificationTime
                }) {
                entryB.addEntryToHistory(history)
            }
        }
        // Last entry not present
        if (entryB.history.find {
                it.lastModificationTime == entryA.lastModificationTime
            } == null) {
            val history = EntryKDBX().apply {
                updateWith(entryA, copyHistory = false, updateParents = false)
                parent = null
            }
            entryB.addEntryToHistory(history)
        }
    }

    /**
     * Utility method to merge a KDBX group
     */
    private fun mergeGroup(nodeToMerge: GroupKDBX, databaseToMerge: DatabaseKDBX) {
        val groupId = nodeToMerge.nodeId
        val group = database.getGroupById(groupId)
        val deletedObject = database.getDeletedObject(groupId)

        databaseToMerge.getGroupById(groupId)?.let { srcGroupToMerge ->
            // Retrieve parent in current database
            var parentGroupToMerge: GroupKDBX? = null
            srcGroupToMerge.parent?.nodeId?.let {
                parentGroupToMerge = database.getGroupById(it)
            }
            val groupToMerge = GroupKDBX().apply {
                updateWith(srcGroupToMerge, updateParents = false)
            }

            if (group == null) {
                // If group parent to add exists and in current database
                if ((deletedObject == null
                    || deletedObject.deletionTime.date
                        .before(groupToMerge.lastModificationTime.date))
                    && parentGroupToMerge != null) {
                    database.addGroupTo(groupToMerge, parentGroupToMerge)
                }
            } else {
                // Merge independently custom data
                mergeCustomData(group.customData, groupToMerge.customData)
                // Merge by modification time
                if (group.lastModificationTime.date
                        .before(groupToMerge.lastModificationTime.date)
                ) {
                    if (parentGroupToMerge == group.parent) {
                        group.updateWith(groupToMerge, false)
                    } else {
                        database.removeGroupFrom(group, group.parent)
                        if (parentGroupToMerge != null) {
                            database.addGroupTo(groupToMerge, parentGroupToMerge)
                        }
                    }
                }
            }
        }
    }
}