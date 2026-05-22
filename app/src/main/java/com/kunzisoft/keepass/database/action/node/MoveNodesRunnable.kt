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
import android.util.Log
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.exception.MissingParentDatabaseException
import com.kunzisoft.keepass.database.exception.MoveEntryDatabaseException
import com.kunzisoft.keepass.database.exception.MoveGroupDatabaseException
import com.kunzisoft.keepass.hardware.HardwareKey
import java.util.UUID

class MoveNodesRunnable(
    context: Context,
    database: ContextualDatabase,
    parentId: NodeId<*>,
    groupsIdsToMove: List<NodeId<*>>,
    entriesIdsToMove: List<NodeId<UUID>>,
    save: Boolean,
    afterActionNodesFinish: AfterActionNodesFinish?,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save, challengeResponseRetriever) {

    private var mOldParent: Group? = null
    private var mNewParent: Group? = null
    private var mGroupsToMove: List<Group> = listOf()
    private var mEntriesToMove: List<Entry> = listOf()

    init {
        database.getGroupById(parentId)?.let { newParent ->
            mNewParent = newParent
        }
        mGroupsToMove = database.getGroupsByIds(groupsIdsToMove)
        mEntriesToMove = database.getEntriesByIds(entriesIdsToMove)
    }

    override fun nodeAction() {
        mNewParent?.let { newParent ->
            foreachGroup@ for (nodeToMove in mGroupsToMove) {
                // Move node in new parent
                mOldParent = nodeToMove.parent
                nodeToMove.touch(modified = true, touchParents = true)
                // Move group if the parent change
                if (mOldParent != newParent
                    // and if not in the current group
                    && nodeToMove != newParent
                    && !newParent.isContainedIn(nodeToMove)
                ) {
                    database.moveGroupTo(nodeToMove, newParent)
                    nodeToMove.setPreviousParentGroup(mOldParent)
                    nodeToMove.touch(modified = true, touchParents = true)
                } else {
                    // Only finish thread
                    setError(MoveGroupDatabaseException())
                    break@foreachGroup
                }
            }
            foreachEntry@ for (nodeToMove in mEntriesToMove) {
                // Move node in new parent
                mOldParent = nodeToMove.parent
                nodeToMove.touch(modified = true, touchParents = true)
                // Move only if the parent change
                if (mOldParent != newParent
                    // and root can contain entry
                    && (newParent != database.rootGroup || database.rootCanContainsEntry())
                ) {
                    database.moveEntryTo(nodeToMove, newParent)
                    nodeToMove.setPreviousParentGroup(mOldParent)
                    nodeToMove.touch(modified = true, touchParents = true)
                } else {
                    // Only finish thread
                    setError(MoveEntryDatabaseException())
                    break@foreachEntry
                }
            }
        } ?: setError(MissingParentDatabaseException())
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            try {
                mOldParent?.let { oldParent ->
                    // If we fail to save, try to move in the first place
                    mGroupsToMove.forEach { nodeToMove ->
                        if (oldParent != nodeToMove.parent) {
                            database.moveGroupTo(nodeToMove, oldParent)
                        }
                    }
                    mEntriesToMove.forEach { nodeToMove ->
                        if (oldParent != nodeToMove.parent) {
                            database.moveEntryTo(nodeToMove, oldParent)
                        }
                    }
                }
            } catch (_: Exception) {
                Log.i(TAG, "Unable to replace the node")
            }
        }
        return ActionNodesValues(
            newGroupsIds = mGroupsToMove.map { it.nodeId },
            newEntriesIds = mEntriesToMove.map { it.nodeId }
        )
    }

    companion object {
        private val TAG = MoveNodesRunnable::class.java.name
    }
}
