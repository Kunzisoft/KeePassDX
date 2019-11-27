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
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node

class UpdateGroupRunnable constructor(
        context: Context,
        database: Database,
        private val mOldGroup: Group,
        private val mNewGroup: Group,
        save: Boolean,
        afterActionNodesFinish: AfterActionNodesFinish?)
    : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save) {

    // Keep backup of original values in case save fails
    private val mBackupGroup: Group = Group(mOldGroup)

    override fun nodeAction() {
        // WARNING : Re attribute parent and children removed in group activity to save memory
        mNewGroup.addParentFrom(mOldGroup)
        mNewGroup.addChildrenFrom(mOldGroup)

        // Update group with new values
        mOldGroup.updateWith(mNewGroup)
        mOldGroup.touch(modified = true, touchParents = true)

        // Only change data in index
        database.updateGroup(mOldGroup)
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            // If we fail to save, back out changes to global structure
            mOldGroup.updateWith(mBackupGroup)
            database.updateGroup(mOldGroup)
        }

        val oldNodesReturn = ArrayList<Node>()
        oldNodesReturn.add(mBackupGroup)
        val newNodesReturn = ArrayList<Node>()
        newNodesReturn.add(mOldGroup)
        return ActionNodesValues(oldNodesReturn, newNodesReturn)
    }
}
