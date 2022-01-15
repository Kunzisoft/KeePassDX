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
        if (database.settingsChanged.date.before(databaseToMerge.settingsChanged.date)) {
            // TODO settings
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

        databaseToMerge.deletedObjects.forEach { deletedObject ->
            val deletedObjectId = deletedObject.uuid
            val databaseEntry = database.getEntryById(deletedObjectId)
            val databaseGroup = database.getGroupById(deletedObjectId)
            val databaseIconModificationTime = database.getCustomIcon(deletedObjectId).lastModificationTime
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
            if (databaseIconModificationTime != null
                && deletedObject.deletionTime.date
                    .after(databaseIconModificationTime.date)) {
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
                if (deletedObject == null
                    || deletedObject.deletionTime.date
                        .before(entryToMerge.lastModificationTime.date)
                    || parentEntryToMerge != null) {
                    database.addEntryTo(entryToMerge, parentEntryToMerge)
                }
            } else if (entry.lastModificationTime.date
                    .before(entryToMerge.lastModificationTime.date)
            ) {
                if (parentEntryToMerge == entry.parent) {
                    entry.updateWith(entryToMerge)
                } else {
                    // Update entry with databaseEntryToMerge and merge history
                    database.removeEntryFrom(entry, entry.parent)
                    // TODO history =
                    if (parentEntryToMerge != null) {
                        database.addEntryTo(entryToMerge, parentEntryToMerge)
                    }
                }
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
                if (deletedObject == null
                    || deletedObject.deletionTime.date
                        .before(groupToMerge.lastModificationTime.date)
                    || parentGroupToMerge != null) {
                    database.addGroupTo(groupToMerge, parentGroupToMerge)
                }
            } else if (group.lastModificationTime.date
                    .before(groupToMerge.lastModificationTime.date)
            ) {
                if (parentGroupToMerge == group.parent) {
                    group.updateWith(groupToMerge)
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