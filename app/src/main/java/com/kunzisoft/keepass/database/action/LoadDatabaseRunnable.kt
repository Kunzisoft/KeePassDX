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
package com.kunzisoft.keepass.database.action

import android.content.ContentResolver
import android.net.Uri
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.exception.LoadDatabaseException
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import java.io.File

class LoadDatabaseRunnable(private val mDatabase: Database,
                           private val mUri: Uri,
                           private val mPass: String?,
                           private val mKey: Uri?,
                           private val contentResolver: ContentResolver,
                           private val cacheDirectory: File,
                           private val mOmitBackup: Boolean,
                           private val mFixDuplicateUUID: Boolean,
                           private val progressTaskUpdater: ProgressTaskUpdater?)
    : ActionRunnable(null, executeNestedActionIfResultFalse = true) {

    override fun run() {
        try {
            mDatabase.loadData(mUri, mPass, mKey,
                    contentResolver,
                    cacheDirectory,
                    mOmitBackup,
                    mFixDuplicateUUID,
                    progressTaskUpdater)
            finishRun(true)
        }
        catch (e: LoadDatabaseException) {
            finishRun(false, e)
        }
    }

    override fun onFinishRun(result: Result) {
        if (!result.isSuccess) {
            mDatabase.closeAndClear(cacheDirectory)
        }
    }
}
