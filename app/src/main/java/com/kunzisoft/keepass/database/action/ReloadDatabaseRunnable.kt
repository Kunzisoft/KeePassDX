/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.action

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.exception.DatabaseException
import com.kunzisoft.keepass.database.exception.UnknownDatabaseLocationException
import com.kunzisoft.keepass.database.helper.MemoryHelper.canMemoryBeAllocatedInRAM
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.UriHelper.getUriInputStream
import com.kunzisoft.keepass.utils.UriUtil.getBinaryDir

class ReloadDatabaseRunnable(
    private val context: Context,
    private val mDatabase: ContextualDatabase,
    private val progressTaskUpdater: ProgressTaskUpdater?,
    private val mLoadDatabaseResult: ((Result) -> Unit)?
) : ActionRunnable() {

    private val binaryDir = context.getBinaryDir()

    override fun onStartRun() {
        // Clear before we load
        mDatabase.clearIndexesAndBinaries(binaryDir)
        mDatabase.wasReloaded = true
    }

    override fun onActionRun() {
        try {
            mDatabase.reloadData(
                context.contentResolver.getUriInputStream(mDatabase.fileUri)
                    ?: throw UnknownDatabaseLocationException(),
                { memoryWanted ->
                    canMemoryBeAllocatedInRAM(context, memoryWanted)
                },
                progressTaskUpdater)
        } catch (e: DatabaseException) {
            setError(e)
        }

        if (result.isSuccess) {
            // Register the current time to init the lock timer
            PreferencesUtil.saveCurrentTime(context)
        } else {
            mDatabase.clearAndClose(binaryDir)
        }
    }

    override fun onFinishRun() {
        mLoadDatabaseResult?.invoke(result)
    }
}
