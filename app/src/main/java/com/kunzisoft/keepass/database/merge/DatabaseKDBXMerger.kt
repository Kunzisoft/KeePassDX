package com.kunzisoft.keepass.database.merge

import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.utils.readAllBytes
import java.io.IOException

class DatabaseKDBXMerger(private var database: DatabaseKDBX) {

    var isRAMSufficient: (memoryWanted: Long) -> Boolean = {true}

    fun merge(databaseToMerge: DatabaseKDBX) {

        // TODO database data
        var nameChanged = DateInstant()
        var settingsChanged = DateInstant()
        var descriptionChanged = DateInstant()
        var defaultUserNameChanged = DateInstant()
        var keyLastChanged = DateInstant()

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

        // TODO fix concurrent modification exception
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

    private fun mergeEntry(entryToMerge: EntryKDBX, databaseToMerge: DatabaseKDBX) {
        val entryId = entryToMerge.nodeId
        val databaseEntryToMerge = databaseToMerge.getEntryById(entryId)
        val databaseEntry = database.getEntryById(entryId)
        val deletedObject = database.getDeletedObject(entryId)

        if (databaseEntryToMerge != null) {
            // Retrieve parent in current database
            var parentEntryToMerge: GroupKDBX? = null
            databaseEntryToMerge.parent?.nodeId?.let {
                parentEntryToMerge = database.getGroupById(it)
            }
            // Copy attachments in main pool
            val newAttachments = mutableListOf<Attachment>()
            databaseEntryToMerge.getAttachments(databaseToMerge.attachmentPool).forEach { attachment ->
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
            databaseEntryToMerge.removeAttachments()
            newAttachments.forEach { newAttachment ->
                databaseEntryToMerge.putAttachment(newAttachment, database.attachmentPool)
            }

            if (databaseEntry == null) {
                // If it's a deleted object, but another instance was updated
                // If entry parent to add exists and in current database
                if (deletedObject == null
                    || deletedObject.deletionTime.date
                        .before(databaseEntryToMerge.lastModificationTime.date)
                    || parentEntryToMerge != null) {
                    database.addEntryTo(databaseEntryToMerge, parentEntryToMerge)
                }
            } else if (databaseEntry.lastModificationTime.date
                    .before(databaseEntryToMerge.lastModificationTime.date)
            ) {
                if (parentEntryToMerge == databaseEntry.parent) {
                    databaseEntry.updateWith(databaseEntryToMerge)
                } else {
                    // Update entry with databaseEntryToMerge and merge history
                    database.removeEntryFrom(databaseEntry, databaseEntry.parent)
                    // TODO history =
                    if (parentEntryToMerge != null) {
                        database.addEntryTo(databaseEntryToMerge, parentEntryToMerge)
                    }
                }
            }
        }
    }

    private fun mergeGroup(node: GroupKDBX, databaseToMerge: DatabaseKDBX) {
        val groupId = node.nodeId
        val databaseGroupToMerge = databaseToMerge.getGroupById(groupId)
        val databaseGroup = database.getGroupById(groupId)
        val deletedObject = database.getDeletedObject(groupId)

        if (databaseGroupToMerge != null) {
            // Retrieve parent in current database
            var parentGroup: GroupKDBX? = null
            databaseGroupToMerge.parent?.nodeId?.let {
                parentGroup = database.getGroupById(it)
            }

            if (databaseGroup == null) {
                // If group parent to add exists and in current database
                if (deletedObject == null
                    || deletedObject.deletionTime.date
                        .before(databaseGroupToMerge.lastModificationTime.date)
                    || parentGroup != null) {
                    database.addGroupTo(databaseGroupToMerge, parentGroup)
                }
            } else if (databaseGroup.lastModificationTime.date
                    .before(databaseGroupToMerge.lastModificationTime.date)
            ) {
                database.removeGroupFrom(databaseGroup, databaseGroup.parent)
                if (parentGroup != null) {
                    database.addGroupTo(databaseGroupToMerge, parentGroup)
                }
            }
        }
    }
}