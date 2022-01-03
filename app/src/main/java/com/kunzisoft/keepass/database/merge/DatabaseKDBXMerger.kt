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
        // TODO Fix Recycle bin

        databaseToMerge.rootGroup?.doForEachChild(
            object : NodeHandler<EntryKDBX>() {
                override fun operate(node: EntryKDBX): Boolean {
                    val entryId = node.nodeId
                    databaseToMerge.getEntryById(entryId)?.let {
                        mergeEntry(database.getEntryById(entryId), it)
                    }
                    return true
                }
            },
            object : NodeHandler<GroupKDBX>() {
                override fun operate(node: GroupKDBX): Boolean {
                    val groupId = node.nodeId
                    databaseToMerge.getGroupById(groupId)?.let {
                        mergeGroup(database.getGroupById(groupId), it)
                    }
                    return true
                }
            }
        )
    }

    private fun mergeEntry(databaseEntry: EntryKDBX?, databaseEntryToMerge: EntryKDBX) {
        // Retrieve parent in current database
        var parentEntry: GroupKDBX? = null
        databaseEntryToMerge.parent?.nodeId?.let {
            parentEntry = database.getGroupById(it)
        }

        if (databaseEntry == null) {
            // TODO if it's not a deleted object
            // If entry parent to add exists and in current database
            if (parentEntry != null) {
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

    private fun mergeGroup(databaseGroup: GroupKDBX?, databaseGroupToMerge: GroupKDBX) {
        // Retrieve parent in current database
        var parentGroup: GroupKDBX? = null
        databaseGroupToMerge.parent?.nodeId?.let {
            parentGroup = database.getGroupById(it)
        }

        if (databaseGroup == null) {
            // TODO if it's not a deleted object
            // If group parent to add exists and in current database
            if (parentGroup != null) {
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