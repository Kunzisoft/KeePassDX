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
import com.kunzisoft.keepass.database.element.PwEntryInterface
import com.kunzisoft.keepass.database.element.PwGroupInterface

class DeleteEntryRunnable constructor(
        context: FragmentActivity,
        database: Database,
        private val mEntryToDelete: PwEntryInterface,
        finishRunnable: AfterActionNodeFinishRunnable?,
        save: Boolean)
    : ActionNodeDatabaseRunnable(context, database, finishRunnable, save) {

    private var mParent: PwGroupInterface? = null
    private var mRecycle: Boolean = false


    override fun nodeAction() {
        mParent = mEntryToDelete.parent
        mParent?.touch(false, true)

        // Remove Entry from parent
        mRecycle = database.canRecycle(mEntryToDelete)
        if (mRecycle) {
            database.recycle(mEntryToDelete)
        } else {
            database.deleteEntry(mEntryToDelete)
        }
    }

    override fun nodeFinish(isSuccess: Boolean, message: String?): ActionNodeValues {
        if (!isSuccess) {
            if (mRecycle) {
                database.undoRecycle(mEntryToDelete, mParent)
            } else {
                database.undoDeleteEntry(mEntryToDelete, mParent)
            }
        }
        return ActionNodeValues(isSuccess, message, mEntryToDelete, null)
    }
}
