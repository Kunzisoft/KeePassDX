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
import com.kunzisoft.keepass.database.Database
import com.kunzisoft.keepass.database.exception.InvalidKeyFileException
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.UriUtil
import java.io.IOException

class AssignPasswordInDatabaseRunnable @JvmOverloads constructor(
        ctx: Context,
        db: Database,
        withMasterPassword: Boolean,
        masterPassword: String?,
        withKeyFile: Boolean,
        keyfile: Uri?,
        actionRunnable: ActionRunnable? = null,
        save: Boolean)
    : SaveDatabaseRunnable(ctx, db, actionRunnable, save) {

    private var mMasterPassword: String? = null
    private var mKeyfile: Uri? = null

    private var mBackupKey: ByteArray? = null

    init {
        if (withMasterPassword)
            this.mMasterPassword = masterPassword
        if (withKeyFile)
            this.mKeyfile = keyfile
    }

    override fun run() {
        // Set key
        try {
            val pm = database.pwDatabase
            // TODO move master key methods
            mBackupKey = ByteArray(pm.getMasterKey().size)
            System.arraycopy(pm.getMasterKey(), 0, mBackupKey!!, 0, mBackupKey!!.size)

            val uriInputStream = UriUtil.getUriInputStream(context, mKeyfile)
            pm.retrieveMasterKey(mMasterPassword, uriInputStream)
            // To save the database
            super.run()
            finishRun(true)
        } catch (e: InvalidKeyFileException) {
            erase(mBackupKey)
            finishRun(false, e.message)
        } catch (e: IOException) {
            erase(mBackupKey)
            finishRun(false, e.message)
        }
    }

    override fun onFinishRun(isSuccess: Boolean, message: String?) {
        if (!isSuccess) {
            // Erase the current master key
            erase(database.pwDatabase.getMasterKey())
            database.pwDatabase.setMasterKey(mBackupKey)
        }

        super.onFinishRun(isSuccess, message)
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
