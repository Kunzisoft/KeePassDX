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
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.model.GroupInfo

class UpdateGroupRunnable(
    context: Context,
    database: ContextualDatabase,
    oldGroupId: NodeId<*>,
    newGroup: GroupInfo,
    save: Boolean,
    afterActionNodesFinish: AfterActionNodesFinish?,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save, challengeResponseRetriever) {

    private var mOldGroup: Group? = null
    private var mNewGroup: Group? = null

    init {
        database.getGroupById(oldGroupId)?.let { oldGroupToUpdate ->
            mOldGroup = oldGroupToUpdate
            // TODO Same groupId
        }
        database.getGroupById(newGroup.nodeId)?.let { oldGroup ->
            mNewGroup = Group(oldGroup).apply {
                setGroupInfo(newGroup)
            }
        }
    }

    override fun nodeAction() {
        mOldGroup?.let { oldGroup ->
            mNewGroup?.let { newGroup ->
                if (oldGroup.nodeId == newGroup.nodeId) {
                    // WARNING : Re attribute parent and children removed in group activity to save memory
                    newGroup.addParentFrom(oldGroup)
                    newGroup.addChildrenFrom(oldGroup)

                    // Update group with new values
                    newGroup.touch(modified = true, touchParents = true)

                    if (database.rootGroup == oldGroup) {
                        database.rootGroup = newGroup
                    }
                    // Only change data in index
                    database.updateGroup(newGroup)
                }
            }
        }
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            // If we fail to save, back out changes to global structure
            if (database.rootGroup == mNewGroup) {
                database.rootGroup = mOldGroup
            }
            mOldGroup?.let { oldGroup ->
                database.updateGroup(oldGroup)
            }
        }
        return ActionNodesValues(
            oldGroupsIds = mOldGroup?.nodeId?.let { listOf(it) },
            newGroupsIds = mNewGroup?.nodeId?.let { listOf(it) }
        )
    }
}
