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

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.*

class CopyNodesRunnable constructor(
        context: FragmentActivity,
        database: Database,
        private val mNodesToCopy: List<NodeVersioned>,
        private val mNewParent: GroupVersioned,
        afterAddNodeRunnable: AfterActionNodeFinishRunnable?,
        save: Boolean)
    : ActionNodeDatabaseRunnable(context, database, afterAddNodeRunnable, save) {

    private var mEntriesCopied = ArrayList<EntryVersioned>()

    override fun nodeAction() {

        mNodesToCopy.forEach { currentNode ->

            when (currentNode.type) {
                Type.GROUP -> Log.e(TAG, "Copy not allowed for group")
                Type.ENTRY -> {
                    // Root can contains entry
                    if (mNewParent != database.rootGroup || database.rootCanContainsEntry()) {
                        // Update entry with new values
                        mNewParent.touch(modified = false, touchParents = true)
                        database.copyEntryTo(currentNode as EntryVersioned, mNewParent)?.let { entryCopied ->
                            entryCopied.touch(modified = true, touchParents = true)
                            mEntriesCopied.add(entryCopied)
                        } ?: Log.e(TAG, "Unable to create a copy of the entry")
                    } else {
                        // Only finish thread
                        throw Exception(context.getString(R.string.error_copy_entry_here))
                    }
                }
            }
        }
    }

    override fun nodeFinish(result: Result): ActionNodeValues {
        if (!result.isSuccess) {
            // If we fail to save, try to delete the copy
            mEntriesCopied.forEach {
                try {
                    database.deleteEntry(it)
                } catch (e: Exception) {
                    Log.i(TAG, "Unable to delete the copied entry")
                }
            }
        }
        return ActionNodeValues(result, mNodesToCopy, mEntriesCopied)
    }

    companion object {
        private val TAG = CopyNodesRunnable::class.java.name
    }
}
