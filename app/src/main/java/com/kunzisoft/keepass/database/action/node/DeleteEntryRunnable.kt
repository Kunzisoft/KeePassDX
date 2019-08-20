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
import android.support.v4.app.FragmentActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.EntryVersioned
import com.kunzisoft.keepass.database.element.GroupVersioned

class DeleteEntryRunnable constructor(
        context: FragmentActivity,
        database: Database,
        private val mEntryToDelete: EntryVersioned,
        finishRunnable: AfterActionNodeFinishRunnable?,
        save: Boolean)
    : ActionNodeDatabaseRunnable(context, database, finishRunnable, save) {

    private var mParent: GroupVersioned? = null
    private var mCanRecycle: Boolean = false

    private var mEntryToDeleteBackup: EntryVersioned? = null
    private var mNodePosition: Int? = null

    override fun nodeAction() {
        mParent = mEntryToDelete.parent
        mParent?.touch(modified = false, touchParents = true)

        // Get the node position
        mNodePosition = mEntryToDelete.nodePositionInParent

        // Create a copy to keep the old ref and remove it visually
        mEntryToDeleteBackup = EntryVersioned(mEntryToDelete)

        // Remove Entry from parent
        mCanRecycle = database.canRecycle(mEntryToDelete)
        if (mCanRecycle) {
            database.recycle(mEntryToDelete, context.resources)
        } else {
            database.deleteEntry(mEntryToDelete)
        }
    }

    override fun nodeFinish(result: Result): ActionNodeValues {
        if (!result.isSuccess) {
            mParent?.let {
                if (mCanRecycle) {
                    database.undoRecycle(mEntryToDelete, it)
                } else {
                    database.undoDeleteEntry(mEntryToDelete, it)
                }
            }
        }

        // Add position in bundle to delete the node in view
        mNodePosition?.let { position ->
            result.data = Bundle().apply {
                putInt(NODE_POSITION_FOR_ACTION_NATURAL_ORDER_KEY, position )
            }
        }

        // Return a copy of unchanged entry as old param
        // and entry deleted or moved in recycle bin as new param
        return ActionNodeValues(result, mEntryToDeleteBackup, mEntryToDelete)
    }
}
