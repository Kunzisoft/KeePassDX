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
import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.UriUtil

class CreateDatabaseRunnable(context: Context,
                             private val mDatabase: Database,
                             databaseUri: Uri,
                             private val databaseName: String,
                             private val rootName: String,
                             withMasterPassword: Boolean,
                             masterPassword: String?,
                             withKeyFile: Boolean,
                             keyFile: Uri?,
                             private val createDatabaseResult: ((Result) -> Unit)?)
    : AssignPasswordInDatabaseRunnable(context, mDatabase, databaseUri, withMasterPassword, masterPassword, withKeyFile, keyFile) {

    override fun onStartRun() {
        try {
            // Create new database record
            mDatabase.apply {
                createData(mDatabaseUri, databaseName, rootName)
            }
        } catch (e: Exception) {
            mDatabase.clearAndClose(UriUtil.getBinaryDir(context))
            setError(e)
        }

        super.onStartRun()
    }

    override fun onActionRun() {
        super.onActionRun()

        if (result.isSuccess) {
            // Add database to recent files
            if (PreferencesUtil.rememberDatabaseLocations(context)) {
                FileDatabaseHistoryAction.getInstance(context.applicationContext)
                        .addOrUpdateDatabaseUri(mDatabaseUri,
                                if (PreferencesUtil.rememberKeyFileLocations(context)) mKeyFileUri else null)
            }

            // Register the current time to init the lock timer
            PreferencesUtil.saveCurrentTime(context)
        } else {
            Log.e("CreateDatabaseRunnable", "Unable to create the database")
        }
    }

    override fun onFinishRun() {
        super.onFinishRun()

        createDatabaseResult?.invoke(result)
    }
}
