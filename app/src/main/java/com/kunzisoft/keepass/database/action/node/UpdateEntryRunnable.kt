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
import com.kunzisoft.keepass.database.element.EntryVersioned

class UpdateEntryRunnable constructor(
        context: FragmentActivity,
        database: Database,
        private val mOldEntry: EntryVersioned,
        private val mNewEntry: EntryVersioned,
        finishRunnable: AfterActionNodeFinishRunnable?,
        save: Boolean)
    : ActionNodeDatabaseRunnable(context, database, finishRunnable, save) {

    // Keep backup of original values in case save fails
    private var mBackupEntry: EntryVersioned? = null

    override fun nodeAction() {
        mBackupEntry = database.addHistoryBackupTo(mOldEntry)
        mOldEntry.touch(true, true)
        // Update entry with new values
        mOldEntry.updateWith(mNewEntry)
    }

    override fun nodeFinish(isSuccess: Boolean, message: String?): ActionNodeValues {
        if (!isSuccess) {
            // If we fail to save, back out changes to global structure
            mBackupEntry?.let {
                mOldEntry.updateWith(it)
            }
        }
        return ActionNodeValues(isSuccess, message, mOldEntry, mNewEntry)
    }
}
