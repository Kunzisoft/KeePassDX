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

import androidx.fragment.app.FragmentActivity
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.EntryVersioned
import com.kunzisoft.keepass.database.element.GroupVersioned

class MoveEntryRunnable constructor(
        context: FragmentActivity,
        database: Database,
        private val mEntryToMove: EntryVersioned?,
        private val mNewParent: GroupVersioned,
        afterAddNodeRunnable: AfterActionNodeFinishRunnable?,
        save: Boolean)
    : ActionNodeDatabaseRunnable(context, database, afterAddNodeRunnable, save) {

    private var mOldParent: GroupVersioned? = null

    override fun nodeAction() {
        // Move entry in new parent
        mEntryToMove?.let {
            mOldParent = it.parent

            // Condition
            var conditionAccepted = true
            if(mNewParent == database.rootGroup && !database.rootCanContainsEntry())
                conditionAccepted = false
            // Move only if the parent change
            if (mOldParent != mNewParent && conditionAccepted) {
                database.moveEntryTo(it, mNewParent)
            } else {
                // Only finish thread
                throw Exception(context.getString(R.string.error_move_entry_here))
            }
            it.touch(modified = true, touchParents = true)
        } ?: Log.e(TAG, "Unable to create a copy of the entry")
    }

    override fun nodeFinish(result: Result): ActionNodeValues {
        if (!result.isSuccess) {
            // If we fail to save, try to remove in the first place
            try {
                if (mEntryToMove != null && mOldParent != null)
                    database.moveEntryTo(mEntryToMove, mOldParent!!)
            } catch (e: Exception) {
                Log.i(TAG, "Unable to replace the entry")
            }

        }
        return ActionNodeValues(result, null, mEntryToMove)
    }

    companion object {
        private val TAG = MoveEntryRunnable::class.java.name
    }
}
