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
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.GroupVersioned

class MoveGroupRunnable constructor(
        context: FragmentActivity,
        database: Database,
        private val mGroupToMove: GroupVersioned?,
        private val mNewParent: GroupVersioned,
        afterAddNodeRunnable: AfterActionNodeFinishRunnable?,
        save: Boolean)
    : ActionNodeDatabaseRunnable(context, database, afterAddNodeRunnable, save) {

    private var mOldParent: GroupVersioned? = null

    override fun nodeAction() {
        mGroupToMove?.let {
            mOldParent = it.parent
            // Move group in new parent if not in the current group
            if (mGroupToMove != mNewParent && !mNewParent.isContainedIn(mGroupToMove)) {
                database.moveGroupTo(mGroupToMove, mNewParent)
                mGroupToMove.touch(true, true)
                finishRun(true)
            } else {
                // Only finish thread
                val message = context.getString(R.string.error_move_folder_in_itself)
                Log.e(TAG, message)
                finishRun(false, message)
            }
        } ?: Log.e(TAG, "Unable to create a copy of the group")
    }

    override fun nodeFinish(isSuccess: Boolean, message: String?): ActionNodeValues {
        if (!isSuccess) {
            // If we fail to save, try to move in the first place
            try {
                if (mGroupToMove != null && mOldParent != null)
                database.moveGroupTo(mGroupToMove, mOldParent!!)
            } catch (e: Exception) {
                Log.i(TAG, "Unable to replace the group")
            }

        }
        return ActionNodeValues(isSuccess, message, null, mGroupToMove)
    }

    companion object {
        private val TAG = MoveGroupRunnable::class.java.name
    }
}
