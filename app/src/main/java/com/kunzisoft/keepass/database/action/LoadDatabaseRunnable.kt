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
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.exception.DatabaseInputException
import com.kunzisoft.keepass.database.exception.UnknownDatabaseLocationException
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.getBinaryDir
import com.kunzisoft.keepass.utils.getUriInputStream

class LoadDatabaseRunnable(
    private val context: Context,
    private val mDatabase: ContextualDatabase,
    private val mDatabaseUri: Uri,
    private val mMainCredential: MainCredential,
    private val mChallengeResponseRetriever: (hardwareKey: HardwareKey, seed: ByteArray?) -> ByteArray,
    private val mReadonly: Boolean,
    private val mFixDuplicateUUID: Boolean,
    private val progressTaskUpdater: ProgressTaskUpdater?
) : ActionRunnable() {

    var afterLoadDatabase : ((Result) -> Unit)? = null

    private val binaryDir = context.getBinaryDir()

    override fun onStartRun() {
        // Clear before we load
        mDatabase.clearAndClose(binaryDir)
    }

    override fun onActionRun() {
        try {
            val contentResolver = context.contentResolver
            // Save database URI
            mDatabase.fileUri = mDatabaseUri
            mDatabase.loadData(
                contentResolver.getUriInputStream(mDatabaseUri)
                    ?: throw UnknownDatabaseLocationException(),
                mMainCredential.toMasterCredential(contentResolver),
                mChallengeResponseRetriever,
                mReadonly,
                binaryDir,
                { memoryWanted ->
                    BinaryData.canMemoryBeAllocatedInRAM(context, memoryWanted)
                },
                mFixDuplicateUUID,
                progressTaskUpdater
            )
        } catch (e: DatabaseInputException) {
            setError(e)
        }

        if (!result.isSuccess) {
            mDatabase.clearAndClose(binaryDir)
        }
    }

    override fun onFinishRun() {
        afterLoadDatabase?.invoke(result)
    }
}
