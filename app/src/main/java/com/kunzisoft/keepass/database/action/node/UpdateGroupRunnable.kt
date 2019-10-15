/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.action.node

import android.content.Context
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.GroupVersioned
import com.kunzisoft.keepass.database.element.NodeVersioned

class UpdateGroupRunnable constructor(
        context: Context,
        database: Database,
        private val mOldGroup: GroupVersioned,
        private val mNewGroup: GroupVersioned,
        save: Boolean,
        finishRunnable: AfterActionNodeFinishRunnable?)
    : ActionNodeDatabaseRunnable(context, database, finishRunnable, save) {

    // Keep backup of original values in case save fails
    private val mBackupGroup: GroupVersioned = GroupVersioned(mOldGroup)

    override fun nodeAction() {
        // Update group with new values
        mOldGroup.updateWith(mNewGroup)
        mOldGroup.touch(modified = true, touchParents = true)

        // Only change data un index
        database.updateGroup(mOldGroup)
    }

    override fun nodeFinish(result: Result): ActionNodeValues {
        if (!result.isSuccess) {
            // If we fail to save, back out changes to global structure
            mOldGroup.updateWith(mBackupGroup)
            database.updateGroup(mOldGroup)
        }

        val oldNodesReturn = ArrayList<NodeVersioned>()
        oldNodesReturn.add(mBackupGroup)
        val newNodesReturn = ArrayList<NodeVersioned>()
        newNodesReturn.add(mOldGroup)
        return ActionNodeValues(result, oldNodesReturn, newNodesReturn)
    }
}
