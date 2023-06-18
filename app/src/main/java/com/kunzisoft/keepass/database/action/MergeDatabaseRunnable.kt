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
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.exception.DatabaseException
import com.kunzisoft.keepass.database.exception.UnknownDatabaseLocationException
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.getUriInputStream

class MergeDatabaseRunnable(
    context: Context,
    private val mDatabaseToMergeUri: Uri?,
    private val mDatabaseToMergeMainCredential: MainCredential?,
    private val mDatabaseToMergeChallengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    database: ContextualDatabase,
    saveDatabase: Boolean,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    private val progressTaskUpdater: ProgressTaskUpdater?,
) : SaveDatabaseRunnable(
    context,
    database,
    saveDatabase,
    null,
    challengeResponseRetriever
) {
    override fun onStartRun() {
        database.wasReloaded = true
        super.onStartRun()
    }

    override fun onActionRun() {
        try {
            val contentResolver = context.contentResolver
            database.mergeData(
                context.contentResolver.getUriInputStream(
                    mDatabaseToMergeUri ?: database.fileUri
                ) ?: throw UnknownDatabaseLocationException(),
                mDatabaseToMergeMainCredential?.toMasterCredential(contentResolver),
                mDatabaseToMergeChallengeResponseRetriever,
                { memoryWanted ->
                    BinaryData.canMemoryBeAllocatedInRAM(context, memoryWanted)
                },
                progressTaskUpdater
            )
        } catch (e: DatabaseException) {
            setError(e)
        }

        super.onActionRun()
    }
}
