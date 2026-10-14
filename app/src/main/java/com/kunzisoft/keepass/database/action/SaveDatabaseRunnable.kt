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
import com.kunzisoft.keepass.coordination.CoordinatorSession
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import java.io.ByteArrayOutputStream
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.database.exception.DatabaseException
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.AppUtil.getLimits
import com.kunzisoft.keepass.utils.clear
import com.kunzisoft.keepass.utils.getUriOutputStream
import java.io.File

open class SaveDatabaseRunnable(
    protected var context: Context,
    protected var database: ContextualDatabase,
    private var save: Boolean,
    private var mainCredential: MainCredential?, // If null, uses composite Key
    protected var challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    private var databaseCopyUri: Uri? = null,
    private var dataModified: Boolean = !save,
    protected var progressTaskUpdater: ProgressTaskUpdater? = null
) : ActionRunnable() {

    private var mMasterCredential: MasterCredential? = null
    var afterSaveDatabase: ((Result) -> Unit)? = null

    private var cachedHardwareResponse: ByteArray? = null
    protected val cachingRetriever: (HardwareKey, ByteArray?) -> ByteArray = { hardwareKey, seed ->
        cachedHardwareResponse ?: challengeResponseRetriever(hardwareKey, seed).also {
            cachedHardwareResponse = it
        }
    }

    override fun onStartRun() {}

    override fun onActionRun() {
        database.checkVersion()
        // Save database in all cases if it's a copy
        if ((databaseCopyUri != null || save) && result.isSuccess) {
            try {
                val contentResolver = context.contentResolver
                mMasterCredential = mainCredential?.toMasterCredential(contentResolver)
                // Coordinator writes must be acknowledged by the server BEFORE
                // marking the database clean. An Android file-provider close
                // callback would be too late for this guarantee.
                val destination = databaseCopyUri ?: database.fileUri
                val coordinated = databaseCopyUri == null && CoordinatorSession.handles(destination)
                val staging = if (coordinated) ByteArrayOutputStream() else null
                database.saveData(
                    cacheFile = File.createTempFile("db_", ".tmp", context.cacheDir),
                    databaseOutputStream = {
                        staging ?: contentResolver.getUriOutputStream(destination)
                    },
                    masterCredential = mMasterCredential,
                    challengeResponseRetriever = cachingRetriever,
                    limits = context.getLimits()
                )
                if (coordinated) {
                    // Publish only after a complete, fsync'ed and atomically
                    // installed encrypted recovery revision is retained.
                    val encrypted = staging!!.toByteArray()
                    CoordinatorSession.persistAndPublish(context, destination!!, encrypted)
                }
                // Indicate data was saved only after remote publication succeeded.
                if (databaseCopyUri == null) {
                    database.indicateUpToDateData()
                }
            } catch (e: Exception) {
                database.indicateNotSavedData()
                setError(if (e is DatabaseException) e else DatabaseOutputException(e))
            }
        } else if (dataModified) {
            database.indicateNotSavedData()
        }
    }

    override fun onFinishRun() {
        // Need to call super.onFinishRun() in child class
        mMasterCredential?.clear()
        cachedHardwareResponse?.clear()
        afterSaveDatabase?.invoke(result)
    }
}
