/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.MainCredential
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.exception.DatabaseException
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater

class MergeDatabaseRunnable(
    context: Context,
    private val mDatabaseToMergeUri: Uri?,
    private val mDatabaseToMergeMainCredential: MainCredential?,
    private val mDatabaseToMergeChallengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    database: Database,
    saveDatabase: Boolean,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    private val progressTaskUpdater: ProgressTaskUpdater?,
    private val mLoadDatabaseResult: ((Result) -> Unit)?
) : SaveDatabaseRunnable(context, database, saveDatabase, null, challengeResponseRetriever) {

    override fun onStartRun() {
        database.wasReloaded = true
        super.onStartRun()
    }

    override fun onActionRun() {
        try {
            database.mergeData(
                context.contentResolver,
                mDatabaseToMergeUri,
                mDatabaseToMergeMainCredential,
                mDatabaseToMergeChallengeResponseRetriever,
                { memoryWanted ->
                    BinaryData.canMemoryBeAllocatedInRAM(context, memoryWanted)
                },
                progressTaskUpdater
            )
        } catch (e: DatabaseException) {
            setError(e)
        }

        if (result.isSuccess) {
            // Register the current time to init the lock timer
            PreferencesUtil.saveCurrentTime(context)
        }
        super.onActionRun()
    }

    override fun onFinishRun() {
        super.onFinishRun()
        mLoadDatabaseResult?.invoke(result)
    }
}
