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
import com.kunzisoft.keepass.database.crypto.kdf.KdfFactory
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.AppUtil.getKdfLimits
import com.kunzisoft.keepass.utils.clear
import com.kunzisoft.keepass.utils.getBinaryDir

class CreateDatabaseRunnable(
    context: Context,
    private val mDatabase: ContextualDatabase,
    private val databaseUri: Uri,
    private val databaseName: String,
    private val rootName: String,
    private val templateGroupName: String?,
    val mainCredential: MainCredential,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    progressTaskUpdater: ProgressTaskUpdater? = null,
) : SaveDatabaseRunnable(
    context,
    mDatabase,
    save = true,
    mainCredential,
    challengeResponseRetriever,
    progressTaskUpdater = progressTaskUpdater
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

    override fun onActionRun() {
        if (result.isSuccess) {
            progressTaskUpdater?.benchmarking()
            try {
                // Perform a security benchmark for a new database creation.
                var kdfEngine = database.kdfEngine
                if (kdfEngine == null) {
                    kdfEngine = KdfFactory.defaultKdf
                }
                kdfEngine.randomize()

                var masterCredential: MasterCredential? = null
                var masterKey: ByteArray? = null
                try {
                    masterCredential = mainCredential.toMasterCredential(context.contentResolver)
                    masterKey = masterCredential.toMasterKey(
                        encoding = database.passwordEncoding,
                        transformSeed = database.transformSeed,
                        challengeResponseRetriever = cachingRetriever
                    )
                    // Calculate max memory directly from the device
                    kdfEngine.optimizeByBenchmark(
                        masterKey = masterKey,
                        limits = context.getKdfLimits()
                    )
                } finally {
                    masterKey?.clear()
                    masterCredential?.clear()
                }
            } catch (e: Exception) {
                setError(e)
            }
        }
        super.onActionRun()
    }

    override fun onFinishRun() {
        if (result.isSuccess) {
            mDatabase.loaded = true
        }
        super.onFinishRun()
    }
}
