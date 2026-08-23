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
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.database.exception.MissingParentDatabaseException
import com.kunzisoft.keepass.database.exception.MoveEntryDatabaseException
import com.kunzisoft.keepass.database.exception.MoveGroupDatabaseException
import com.kunzisoft.keepass.hardware.HardwareKey

class MoveNodesRunnable(
    context: Context,
    database: ContextualDatabase,
    parentId: GroupId,
    groupsIdsToMove: List<GroupId>,
    entriesIdsToMove: List<EntryId>,
    save: Boolean,
    afterActionNodesFinish: AfterActionNodesFinish?,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionNodeDatabaseRunnable(
    context,
    database,
    afterActionNodesFinish,
    save,
    challengeResponseRetriever
) {

    private var mNewParent: Group? = null
    private var mGroupsToMove: List<Group> = listOf()
    private var mEntriesToMove: List<Entry> = listOf()

    private val mOriginalGroupParents = mutableMapOf<GroupId, Group>()
    private val mOriginalEntryParents = mutableMapOf<EntryId, Group>()

    init {
        database.getGroupById(parentId)?.let { newParent ->
            mNewParent = newParent
        }
        mGroupsToMove = database.getGroupsByIds(groupsIdsToMove)
        mEntriesToMove = database.getEntriesByIds(entriesIdsToMove)
    }

    override fun nodeAction() {
        val newParent = mNewParent ?: run {
            setError(MissingParentDatabaseException())
            return
        }

        for (nodeToMove in mGroupsToMove) {
            val oldParent = nodeToMove.parent ?: continue
            nodeToMove.touch(modified = true, touchParents = true)
            // Move group if the parent change
            if (oldParent != newParent
                // and if not in the current group
                && nodeToMove != newParent
                && !newParent.isContainedIn(nodeToMove)
            ) {
                mOriginalGroupParents[nodeToMove.nodeId] = oldParent
                database.moveGroupTo(nodeToMove, newParent)
                nodeToMove.setPreviousParentGroup(oldParent)
                nodeToMove.touch(modified = true, touchParents = true)
            } else {
                setError(MoveGroupDatabaseException())
                return
            }
        }

        for (nodeToMove in mEntriesToMove) {
            val oldParent = nodeToMove.parent ?: continue
            nodeToMove.touch(modified = true, touchParents = true)
            // Move only if the parent change
            if (oldParent != newParent
                // and root can contain entry
                && (newParent != database.rootGroup || database.rootCanContainsEntry())
            ) {
                mOriginalEntryParents[nodeToMove.nodeId] = oldParent
                database.moveEntryTo(nodeToMove, newParent)
                nodeToMove.setPreviousParentGroup(oldParent)
                nodeToMove.touch(modified = true, touchParents = true)
            } else {
                setError(MoveEntryDatabaseException())
                return
            }
        }
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            try {
                // Restore groups in reverse order of move
                mGroupsToMove.reversed().forEach { nodeToMove ->
                    mOriginalGroupParents[nodeToMove.nodeId]?.let { originalParent ->
                        if (nodeToMove.parent != originalParent) {
                            database.moveGroupTo(nodeToMove, originalParent)
                        }
                    }
                }
                // Restore entries
                mEntriesToMove.reversed().forEach { nodeToMove ->
                    mOriginalEntryParents[nodeToMove.nodeId]?.let { originalParent ->
                        if (nodeToMove.parent != originalParent) {
                            database.moveEntryTo(nodeToMove, originalParent)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to restore the nodes", e)
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
