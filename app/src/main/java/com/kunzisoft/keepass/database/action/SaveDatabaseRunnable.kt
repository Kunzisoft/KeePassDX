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
import com.kunzisoft.keepass.database.exception.DatabaseException
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.getUriOutputStream
import java.io.File

open class SaveDatabaseRunnable(
    protected var context: Context,
    protected var database: ContextualDatabase,
    private var saveDatabase: Boolean,
    private var mainCredential: MainCredential?, // If null, uses composite Key
    private var challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    private var databaseCopyUri: Uri? = null
) : ActionRunnable() {

    var afterSaveDatabase: ((Result) -> Unit)? = null

    override fun onStartRun() {}

    override fun onActionRun() {
        database.checkVersion()
        // Save database in all cases if it's a copy
        if ((databaseCopyUri != null || saveDatabase) && result.isSuccess) {
            try {
                val contentResolver = context.contentResolver
                // Build temp database file to avoid file corruption if error
                database.saveData(
                    cacheFile = File(context.cacheDir, databaseCopyUri.hashCode().toString()),
                    databaseOutputStream = {
                        contentResolver
                            .getUriOutputStream(databaseCopyUri ?: database.fileUri)
                    },
                    isNewLocation = databaseCopyUri == null,
                    mainCredential?.toMasterCredential(contentResolver),
                    challengeResponseRetriever)
            } catch (e: DatabaseException) {
                setError(e)
            }
        }
    }

    override fun onFinishRun() {
        // Need to call super.onFinishRun() in child class
        afterSaveDatabase?.invoke(result)
    }
}
