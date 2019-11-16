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
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.utils.UriUtil

open class AssignPasswordInDatabaseRunnable (
        context: Context,
        database: Database,
        protected val mDatabaseUri: Uri,
        withMasterPassword: Boolean,
        masterPassword: String?,
        withKeyFile: Boolean,
        keyFile: Uri?)
    : SaveDatabaseRunnable(context, database, true) {

    private var mMasterPassword: String? = null
    protected var mKeyFile: Uri? = null

    private var mBackupKey: ByteArray? = null

    init {
        if (withMasterPassword)
            this.mMasterPassword = masterPassword
        if (withKeyFile)
            this.mKeyFile = keyFile
    }

    override fun onStartRun() {
        // Set key
        try {
            // TODO move master key methods
            mBackupKey = ByteArray(database.masterKey.size)
            System.arraycopy(database.masterKey, 0, mBackupKey!!, 0, mBackupKey!!.size)

            val uriInputStream = UriUtil.getUriInputStream(context.contentResolver, mKeyFile)
            database.retrieveMasterKey(mMasterPassword, uriInputStream)
        } catch (e: Exception) {
            erase(mBackupKey)
            setError(e.message)
        }

        super.onStartRun()
    }

    override fun onFinishRun() {
        super.onFinishRun()

        // Erase the biometric
        CipherDatabaseAction.getInstance(context)
                .deleteByDatabaseUri(mDatabaseUri)

        if (!result.isSuccess) {
            // Erase the current master key
            erase(database.masterKey)
            mBackupKey?.let {
                database.masterKey = it
            }
        }
    }

    /**
     * Overwrite the array as soon as we don't need it to avoid keeping the extra data in memory
     */
    private fun erase(array: ByteArray?) {
        if (array == null) return
        for (i in array.indices) {
            array[i] = 0
        }
    }
}
