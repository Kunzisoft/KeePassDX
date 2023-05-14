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
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.hardware.HardwareKey

class UpdateGroupRunnable constructor(
    context: Context,
    database: ContextualDatabase,
    private val mOldGroup: Group,
    private val mNewGroup: Group,
    save: Boolean,
    afterActionNodesFinish: AfterActionNodesFinish?,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save, challengeResponseRetriever) {

    override fun nodeAction() {
        if (mOldGroup.nodeId == mNewGroup.nodeId) {
            // WARNING : Re attribute parent and children removed in group activity to save memory
            mNewGroup.addParentFrom(mOldGroup)
            mNewGroup.addChildrenFrom(mOldGroup)

            // Update group with new values
            mNewGroup.touch(modified = true, touchParents = true)

            if (database.rootGroup == mOldGroup) {
                database.rootGroup = mNewGroup
            }
            // Only change data in index
            database.updateGroup(mNewGroup)
        }
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            // If we fail to save, back out changes to global structure
            if (database.rootGroup == mNewGroup) {
                database.rootGroup = mOldGroup
            }
            database.updateGroup(mOldGroup)
        }

        val oldNodesReturn = ArrayList<Node>()
        oldNodesReturn.add(mOldGroup)
        val newNodesReturn = ArrayList<Node>()
        newNodesReturn.add(mNewGroup)
        return ActionNodesValues(oldNodesReturn, newNodesReturn)
    }
}
