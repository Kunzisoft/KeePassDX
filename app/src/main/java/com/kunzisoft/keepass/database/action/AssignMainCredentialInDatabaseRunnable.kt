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
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.hardware.HardwareKey

open class AssignMainCredentialInDatabaseRunnable (
    context: Context,
    database: ContextualDatabase,
    protected val mDatabaseUri: Uri,
    mainCredential: MainCredential,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : SaveDatabaseRunnable(context, database, true, mainCredential, challengeResponseRetriever) {

    private var mBackupKey: ByteArray? = null

    override fun onStartRun() {
        // Set key
        try {
            mBackupKey = ByteArray(database.masterKey.size)
            database.masterKey.copyInto(mBackupKey!!)
        } catch (e: Exception) {
            erase(mBackupKey)
            setError(e)
        }

        super.onStartRun()
    }

    override fun onFinishRun() {
        super.onFinishRun()

        // Erase the biometric
        CipherDatabaseAction.getInstance(context)
                .deleteByDatabaseUri(mDatabaseUri)
        // Erase the register keyfile
        FileDatabaseHistoryAction.getInstance(context)
                .deleteKeyFileByDatabaseUri(mDatabaseUri)

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
