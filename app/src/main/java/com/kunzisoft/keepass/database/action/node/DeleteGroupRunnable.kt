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

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.GroupVersioned

class DeleteGroupRunnable(context: FragmentActivity,
                          database: Database,
                          private val mGroupToDelete: GroupVersioned,
                          finish: AfterActionNodeFinishRunnable,
                          save: Boolean) : ActionNodeDatabaseRunnable(context, database, finish, save) {
    private var mParent: GroupVersioned? = null
    private var mRecycle: Boolean = false

    private var mGroupToDeleteBackup: GroupVersioned? = null
    private var mNodePosition: Int? = null

    override fun nodeAction() {
        mParent = mGroupToDelete.parent
        mParent?.touch(modified = false, touchParents = true)

        // Get the node position
        mNodePosition = mGroupToDelete.nodePositionInParent

        // Create a copy to keep the old ref and remove it visually
        mGroupToDeleteBackup = GroupVersioned(mGroupToDelete)

        // Remove Group from parent
        mRecycle = database.canRecycle(mGroupToDelete)
        if (mRecycle) {
            database.recycle(mGroupToDelete, context.resources)
        } else {
            database.deleteGroup(mGroupToDelete)
        }
    }

    override fun nodeFinish(result: Result): ActionNodeValues {
        if (!result.isSuccess) {
            if (mRecycle) {
                mParent?.let {
                    database.undoRecycle(mGroupToDelete, it)
                }
            }
            // else {
                // Let's not bother recovering from a failure to save a deleted tree. It is too much work.
                // TODO database.undoDeleteGroupFrom(mGroup, mParent);
            // }
        }

        // Add position in bundle to delete the node in view
        mNodePosition?.let { position ->
            result.data = Bundle().apply {
                putInt(NODE_POSITION_FOR_ACTION_NATURAL_ORDER_KEY, position )
            }
        } ?: run {
            result.data?.remove(NODE_POSITION_FOR_ACTION_NATURAL_ORDER_KEY)
        }

        // Return a copy of unchanged group as old param
        // and group deleted or moved in recycle bin as new param
        return ActionNodeValues(result, mGroupToDeleteBackup, mGroupToDelete)
    }
}
