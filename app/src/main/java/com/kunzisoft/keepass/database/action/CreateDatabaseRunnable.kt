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

import android.content.Context
import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.database.element.Database

class CreateDatabaseRunnable(context: Context,
                             private val mDatabase: Database,
                             databaseUri: Uri,
                             withMasterPassword: Boolean,
                             masterPassword: String?,
                             withKeyFile: Boolean,
                             keyFile: Uri?,
                             save: Boolean)
    : AssignPasswordInDatabaseRunnable(context, mDatabase, databaseUri, withMasterPassword, masterPassword, withKeyFile, keyFile, save) {

    override fun onStartRun() {
        try {
            // Create new database record
            mDatabase.apply {
                createData(mDatabaseUri)
                // Set Database state
                loaded = true
            }
        } catch (e: Exception) {
            mDatabase.closeAndClear()
            setError(e.message)
        }

        super.onStartRun()
    }

    override fun onFinishRun() {
        super.onFinishRun()

        if (result.isSuccess) {
            // Add database to recent files
            FileDatabaseHistoryAction.getInstance(context.applicationContext)
                    .addOrUpdateDatabaseUri(mDatabaseUri, mKeyFile)
        } else {
            Log.e("CreateDatabaseRunnable", "Unable to create the database")
        }
    }
}
