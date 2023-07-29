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
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.getBinaryDir

class CreateDatabaseRunnable(
    context: Context,
    private val mDatabase: ContextualDatabase,
    private val databaseUri: Uri,
    private val databaseName: String,
    private val rootName: String,
    private val templateGroupName: String?,
    val mainCredential: MainCredential,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : SaveDatabaseRunnable(
    context,
    mDatabase,
    true,
    mainCredential,
    challengeResponseRetriever
) {
    override fun onStartRun() {
        try {
            // Create new database record
            mDatabase.apply {
                this.fileUri = databaseUri
                createData(databaseName, rootName, templateGroupName)
            }
        } catch (e: Exception) {
            mDatabase.clearAndClose(context.getBinaryDir())
            setError(e)
        }

        super.onStartRun()
    }

    override fun onFinishRun() {
        if (result.isSuccess) {
            mDatabase.loaded = true
        }
        super.onFinishRun()
    }
}
