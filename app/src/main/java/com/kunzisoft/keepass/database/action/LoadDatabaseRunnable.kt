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
import android.content.Intent
import android.net.Uri
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.exception.LoadDatabaseException
import com.kunzisoft.keepass.notifications.DatabaseOpenNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater

class LoadDatabaseRunnable(private val context: Context,
                           private val mDatabase: Database,
                           private val mUri: Uri,
                           private val mPass: String?,
                           private val mKey: Uri?,
                           private val mReadonly: Boolean,
                           private val mCipherEntity: CipherDatabaseEntity?,
                           private val mOmitBackup: Boolean,
                           private val mFixDuplicateUUID: Boolean,
                           private val progressTaskUpdater: ProgressTaskUpdater?,
                           private val mOnFinish: ((Result) -> Unit)?)
    : ActionRunnable(null, executeNestedActionIfResultFalse = true) {

    private val cacheDirectory = context.applicationContext.filesDir

    override fun run() {
        try {
            // Clear before we load
            mDatabase.closeAndClear(cacheDirectory)

            mDatabase.loadData(mUri, mPass, mKey,
                    mReadonly,
                    context.contentResolver,
                    cacheDirectory,
                    mOmitBackup,
                    mFixDuplicateUUID,
                    progressTaskUpdater)

            // Save keyFile in app database
            val rememberKeyFile = PreferencesUtil.rememberKeyFiles(context)
            if (rememberKeyFile) {
                var keyUri = mKey
                if (!rememberKeyFile) {
                    keyUri = null
                }
                FileDatabaseHistoryAction.getInstance(context)
                        .addOrUpdateDatabaseUri(mUri, keyUri)
            }

            mOnFinish?.invoke(result)

            // Register the biometric
            mCipherEntity?.let { cipherDatabaseEntity ->
                CipherDatabaseAction.getInstance(context)
                        .addOrUpdateCipherDatabase(cipherDatabaseEntity) {
                            finishRun(true)
                        }
            } ?: run {
                finishRun(true)
            }
        }
        catch (e: LoadDatabaseException) {
            finishRun(false, e)
        }

        // Start the opening notification
        context.startService(Intent(context, DatabaseOpenNotificationService::class.java))
    }

    override fun onFinishRun(result: Result) {
        if (!result.isSuccess) {
            mDatabase.closeAndClear(cacheDirectory)
        }
    }
}
