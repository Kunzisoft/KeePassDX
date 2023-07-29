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
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.hardware.HardwareKey

class DeleteNodesRunnable(
    context: Context,
    database: ContextualDatabase,
    private val mNodesToDelete: List<Node>,
    private val recyclerBinTitle: String,
    save: Boolean,
    afterActionNodesFinish: AfterActionNodesFinish,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save, challengeResponseRetriever) {

    private var mOldParent: Group? = null
    private var mCanRecycle: Boolean = false

    private var mNodesToDeleteBackup = ArrayList<Node>()

    override fun nodeAction() {

        foreachNode@ for(nodeToDelete in mNodesToDelete) {
            mOldParent = nodeToDelete.parent
            nodeToDelete.touch(modified = true, touchParents = true)

            when (nodeToDelete.type) {
                Type.GROUP -> {
                    val groupToDelete = nodeToDelete as Group
                    // Create a copy to keep the old ref and remove it visually
                    mNodesToDeleteBackup.add(Group(groupToDelete))
                    // Remove Node from parent
                    mCanRecycle = database.canRecycle(groupToDelete)
                    if (mCanRecycle) {
                        database.recycle(groupToDelete, recyclerBinTitle)
                        groupToDelete.setPreviousParentGroup(mOldParent)
                        groupToDelete.touch(modified = true, touchParents = true)
                    } else {
                        database.deleteGroup(groupToDelete)
                    }
                }
                Type.ENTRY -> {
                    val entryToDelete = nodeToDelete as Entry
                    // Create a copy to keep the old ref and remove it visually
                    mNodesToDeleteBackup.add(Entry(entryToDelete))
                    // Remove Node from parent
                    mCanRecycle = database.canRecycle(entryToDelete)
                    if (mCanRecycle) {
                        database.recycle(entryToDelete, recyclerBinTitle)
                        entryToDelete.setPreviousParentGroup(mOldParent)
                        entryToDelete.touch(modified = true, touchParents = true)
                    } else {
                        database.deleteEntry(entryToDelete)
                    }
                    // Remove the oldest attachments
                    entryToDelete.getAttachments(database.attachmentPool).forEach {
                        database.removeAttachmentIfNotUsed(it)
                    }
                }
            }
        }
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            if (mCanRecycle) {
                mOldParent?.let {
                    mNodesToDeleteBackup.forEach { backupNode ->
                        when (backupNode.type) {
                            Type.GROUP -> {
                                database.undoRecycle(backupNode as Group, it)
                            }
                            Type.ENTRY -> {
                                database.undoRecycle(backupNode as Entry, it)
                            }
                        }
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
        return ActionNodesValues(mNodesToDeleteBackup, mNodesToDelete)
    }
}
