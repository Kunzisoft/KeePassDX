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

import android.support.v4.app.FragmentActivity

import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.GroupVersioned

class DeleteGroupRunnable(context: FragmentActivity,
                          database: Database,
                          private val mGroupToDelete: GroupVersioned,
                          finish: AfterActionNodeFinishRunnable,
                          save: Boolean) : ActionNodeDatabaseRunnable(context, database, finish, save) {
    private var mParent: GroupVersioned? = null
    private var mRecycle: Boolean = false

    override fun nodeAction() {
        mParent = mGroupToDelete.parent
        mParent?.touch(false, true)

        // Remove Group from parent
        mRecycle = database.canRecycle(mGroupToDelete)
        if (mRecycle) {
            database.recycle(mGroupToDelete)
        } else {
            database.deleteGroup(mGroupToDelete)
        }
    }

    override fun nodeFinish(isSuccess: Boolean, message: String?): ActionNodeValues {
        if (!isSuccess) {
            if (mRecycle) {
                mParent?.let {
                    database.undoRecycle(mGroupToDelete, it)
                }
            } else {
                // Let's not bother recovering from a failure to save a deleted tree. It is too much work.
                // TODO database.undoDeleteGroupFrom(mGroup, mParent);
            }
        }
        return ActionNodeValues(isSuccess, message, mGroupToDelete, null)
    }
}
