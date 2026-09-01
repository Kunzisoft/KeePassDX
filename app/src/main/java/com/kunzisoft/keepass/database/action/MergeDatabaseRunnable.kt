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
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.database.exception.DatabaseException
import com.kunzisoft.keepass.database.exception.UnknownDatabaseLocationException
import com.kunzisoft.keepass.hardware.ChallengeRequest
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.AppUtil.getLimits
import com.kunzisoft.keepass.utils.AppUtil.getRelyingPartyId
import com.kunzisoft.keepass.utils.getUriInputStream

class MergeDatabaseRunnable(
    context: Context,
    private val mDatabaseToMergeUri: Uri?,
    private val mDatabaseToMergeMainCredential: MainCredential?,
    private val mDatabaseToMergeChallengeResponseRetriever: (ChallengeRequest) -> ByteArray,
    database: ContextualDatabase,
    save: Boolean,
    challengeResponseRetriever: (ChallengeRequest) -> ByteArray,
    progressTaskUpdater: ProgressTaskUpdater?
) : SaveDatabaseRunnable(
    context = context,
    database = database,
    save = save,
    mainCredential = null,
    challengeOperation = ChallengeRequest.ChallengeOperation.UPDATE,
    challengeResponseRetriever = challengeResponseRetriever,
    progressTaskUpdater = progressTaskUpdater
) {

    private var mMergeMasterCredential: MasterCredential? = null

    override fun onStartRun() {
        database.wasReloaded = true
        super.onStartRun()
    }

    override fun onActionRun() {
        try {
            val contentResolver = context.contentResolver
            mMergeMasterCredential = mDatabaseToMergeMainCredential?.toMasterCredential(contentResolver)
            database.mergeData(
                databaseToMergeStream = contentResolver.getUriInputStream(
                    mDatabaseToMergeUri ?: database.fileUri
                ) ?: throw UnknownDatabaseLocationException(),
                databaseToMergeMasterCredential = mMergeMasterCredential,
                databaseToMergeChallengeResponseRetriever = mDatabaseToMergeChallengeResponseRetriever,
                relyingPartyId = context.getRelyingPartyId(),
                limits = context.getLimits(),
                progressTaskUpdater = progressTaskUpdater
            )
        } catch (e: DatabaseException) {
            setError(e)
        }

        super.onActionRun()
    }

    override fun onFinishRun() {
        mMergeMasterCredential?.clear()
        super.onFinishRun()
    }
}
