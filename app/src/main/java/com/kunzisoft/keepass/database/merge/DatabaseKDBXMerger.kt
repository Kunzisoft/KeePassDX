package com.kunzisoft.keepass.database.merge

import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import java.io.IOException

class DatabaseKDBXMerger(private var database: DatabaseKDBX) {

    fun merge(databaseToMerge: DatabaseKDBX) {

        val databaseRootGroupId = database.rootGroup?.nodeId
        val databaseRootGroupIdToMerge = databaseToMerge.rootGroup?.nodeId

        if (databaseRootGroupId == null || databaseRootGroupIdToMerge == null) {
            throw IOException("Database is not open")
        }

        // UUID of the root group to merge is unknown
        if (database.getGroupById(databaseRootGroupIdToMerge) == null) {
            // Change it to copy children database root
            // TODO Test
            databaseToMerge.rootGroup?.let { databaseRootGroupToMerge ->
                databaseToMerge.removeGroupIndex(databaseRootGroupToMerge)
                databaseRootGroupToMerge.nodeId = databaseRootGroupId
                databaseToMerge.updateGroup(databaseRootGroupToMerge)
            }
        }

        // TODO Merge icons and attachments

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
        }
    }

    private fun mergeEntry(node: EntryKDBX, databaseToMerge: DatabaseKDBX) {
        val entryId = node.nodeId
        val databaseEntryToMerge = databaseToMerge.getEntryById(entryId)
        val databaseEntry = database.getEntryById(entryId)
        val deletedObject = database.getDeletedObject(entryId)

        if (databaseEntryToMerge != null) {
            // Retrieve parent in current database
            var parentEntry: GroupKDBX? = null
            databaseEntryToMerge.parent?.nodeId?.let {
                parentEntry = database.getGroupById(it)
            }

            if (databaseEntry == null) {
                // If it's a deleted object, but another instance was updated
                // If entry parent to add exists and in current database
                if (deletedObject == null
                    || deletedObject.deletionTime.date
                        .before(databaseEntryToMerge.lastModificationTime.date)
                    || parentEntry != null) {
                    database.addEntryTo(databaseEntryToMerge, parentEntry)
                }
            } else if (databaseEntry.lastModificationTime.date
                    .before(databaseEntryToMerge.lastModificationTime.date)
            ) {
                // Update entry with databaseEntryToMerge and merge history
                database.removeEntryFrom(databaseEntry, databaseEntry.parent)
                val newDatabaseEntry = EntryKDBX().apply {
                    updateWith(databaseEntryToMerge)
                    // TODO history =
                }
                if (parentEntry != null) {
                    database.addEntryTo(newDatabaseEntry, parentEntry)
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