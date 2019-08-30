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

class CopyEntryRunnable constructor(
        context: FragmentActivity,
        database: Database,
        private val mEntryToCopy: EntryVersioned,
        private val mNewParent: GroupVersioned,
        afterAddNodeRunnable: AfterActionNodeFinishRunnable?,
        save: Boolean)
    : ActionNodeDatabaseRunnable(context, database, afterAddNodeRunnable, save) {

    private var mEntryCopied: EntryVersioned? = null

    override fun nodeAction() {
        // Condition
        var conditionAccepted = true
        if(mNewParent == database.rootGroup && !database.rootCanContainsEntry())
            conditionAccepted = false
        if (conditionAccepted) {
            // Update entry with new values
            mNewParent.touch(modified = false, touchParents = true)
            mEntryCopied = database.copyEntryTo(mEntryToCopy, mNewParent)
        } else {
            // Only finish thread
            throw Exception(context.getString(R.string.error_copy_entry_here))
        }

        mEntryCopied?.apply {
            touch(modified = true, touchParents = true)
        } ?: Log.e(TAG, "Unable to create a copy of the entry")
    }

    override fun nodeFinish(result: Result): ActionNodeValues {
        if (!result.isSuccess) {
            // If we fail to save, try to delete the copy
            try {
                mEntryCopied?.let {
                    database.deleteEntry(it)
                }
            } catch (e: Exception) {
                Log.i(TAG, "Unable to delete the copied entry")
            }
        }
        return ActionNodeValues(result, mEntryToCopy, mEntryCopied)
    }

    companion object {
        private val TAG = CopyEntryRunnable::class.java.name
    }
}
