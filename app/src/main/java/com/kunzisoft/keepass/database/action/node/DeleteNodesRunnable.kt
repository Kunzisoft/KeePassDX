/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.action.node

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.hardware.HardwareKey

class DeleteNodesRunnable(
    context: Context,
    database: ContextualDatabase,
    groupsIdsToDelete: List<GroupId>,
    entriesIdsToDelete: List<EntryId>,
    private val recyclerBinTitle: String,
    save: Boolean,
    afterActionNodesFinish: AfterActionNodesFinish,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save, challengeResponseRetriever) {

    private var mOldParent: Group? = null
    private var mCanRecycle: Boolean = false

    private var mGroupsToDelete: List<Group> = listOf()
    private var mEntriesToDelete: List<Entry> = listOf()
    private var mGroupsToDeleteBackup = mutableListOf<Group>()
    private var mEntriesToDeleteBackup = mutableListOf<Entry>()

    init {
        mGroupsToDelete = database.getGroupsByIds(groupsIdsToDelete)
        mEntriesToDelete = database.getEntriesByIds(entriesIdsToDelete)
    }

    override fun nodeAction() {

        for(nodeToDelete in mGroupsToDelete) {
            mOldParent = nodeToDelete.parent
            nodeToDelete.touch(modified = true, touchParents = true)
            // Create a copy to keep the old ref and remove it visually
            mGroupsToDeleteBackup.add(Group(nodeToDelete))
            // Remove Node from parent
            mCanRecycle = database.canRecycle(nodeToDelete)
            if (mCanRecycle) {
                database.recycle(nodeToDelete, recyclerBinTitle)
                nodeToDelete.setPreviousParentGroup(mOldParent)
                nodeToDelete.touch(modified = true, touchParents = true)
            } else {
                database.deleteGroup(nodeToDelete)
            }
        }

        for(nodeToDelete in mEntriesToDelete) {
            mOldParent = nodeToDelete.parent
            nodeToDelete.touch(modified = true, touchParents = true)
            // Create a copy to keep the old ref and remove it visually
            mEntriesToDeleteBackup.add(Entry(nodeToDelete))
            // Remove Node from parent
            mCanRecycle = database.canRecycle(nodeToDelete)
            if (mCanRecycle) {
                database.recycle(nodeToDelete, recyclerBinTitle)
                nodeToDelete.setPreviousParentGroup(mOldParent)
                nodeToDelete.touch(modified = true, touchParents = true)
            } else {
                database.deleteEntry(nodeToDelete)
            }
            // Remove the oldest attachments
            nodeToDelete.getAttachments(database.attachmentPool).forEach {
                database.removeAttachmentIfNotUsed(it)
            }
        }
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            if (mCanRecycle) {
                mOldParent?.let {
                    mGroupsToDeleteBackup.forEach { backupNode ->
                        database.undoRecycle(backupNode, it)
                    }
                    mEntriesToDeleteBackup.forEach { backupNode ->
                        database.undoRecycle(backupNode, it)
                    }
                }
            }
            // else {
                // Let's not bother recovering from a failure to save a deleted tree. It is too much work.
                // TODO database.undoDeleteGroupFrom(mGroup, mParent);
            // }
        }

        // Return a copy of unchanged nodes as old param
        // and nodes deleted or moved in recycle bin as new param
        return ActionNodesValues(
            oldGroupsIds = mGroupsToDeleteBackup.map { it.nodeId },
            oldEntriesIds = mEntriesToDeleteBackup.map { it.nodeId },
            newGroupsIds = mGroupsToDelete.map { it.nodeId },
            newEntriesIds = mEntriesToDelete.map { it.nodeId }
        )
    }
}
