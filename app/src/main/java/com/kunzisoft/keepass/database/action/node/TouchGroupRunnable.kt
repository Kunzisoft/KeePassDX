/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.hardware.HardwareKey

class TouchGroupRunnable(
    context: Context,
    database: ContextualDatabase,
    groupId: GroupId,
    afterActionNodesFinish: AfterActionNodesFinish?,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionNodeDatabaseRunnable(
    context,
    database,
    afterActionNodesFinish,
    save = false,
    challengeResponseRetriever
) {

    private var mGroup: Group? = null

    init {
        database.getGroupById(groupId)?.let { oldGroupToUpdate ->
            mGroup = oldGroupToUpdate
        }
    }

    override fun nodeAction() {
        mGroup?.let { group ->
            group.touch(modified = false, touchParents = true)
            database.updateGroup(group = group, dataModified = false)
        }
    }

    override fun nodeFinish(): ActionNodesValues {
        return ActionNodesValues(
            oldGroupsIds = mGroup?.nodeId?.let { listOf(it) }
        )
    }
}
