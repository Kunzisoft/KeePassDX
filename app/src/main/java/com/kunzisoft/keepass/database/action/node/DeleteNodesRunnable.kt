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
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.Type

class DeleteNodesRunnable(context: Context,
                          database: Database,
                          private val mNodesToDelete: List<Node>,
                          save: Boolean,
                          afterActionNodesFinish: AfterActionNodesFinish)
    : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save) {

    private var mParent: Group? = null
    private var mCanRecycle: Boolean = false

    private var mNodesToDeleteBackup = ArrayList<Node>()

    override fun nodeAction() {

        foreachNode@ for(currentNode in mNodesToDelete) {
            mParent = currentNode.parent
            mParent?.touch(modified = false, touchParents = true)

            when (currentNode.type) {
                Type.GROUP -> {
                    // Create a copy to keep the old ref and remove it visually
                    mNodesToDeleteBackup.add(Group(currentNode as Group))
                    // Remove Node from parent
                    mCanRecycle = database.canRecycle(currentNode)
                    if (mCanRecycle) {
                        database.recycle(currentNode, context.resources)
                    } else {
                        database.deleteGroup(currentNode)
                    }
                }
                Type.ENTRY -> {
                    // Create a copy to keep the old ref and remove it visually
                    mNodesToDeleteBackup.add(Entry(currentNode as Entry))
                    // Remove Node from parent
                    mCanRecycle = database.canRecycle(currentNode)
                    if (mCanRecycle) {
                        database.recycle(currentNode, context.resources)
                    } else {
                        database.deleteEntry(currentNode)
                    }
                }
            }
        }
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            if (mCanRecycle) {
                mParent?.let {
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
