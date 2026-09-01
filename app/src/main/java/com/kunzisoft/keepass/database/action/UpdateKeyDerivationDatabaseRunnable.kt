/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.hardware.ChallengeRequest
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.AppUtil.getKdfLimits

/**
 * Runnable to update KDF engine and perform benchmark if default parameters are used.
 */
class UpdateKeyDerivationDatabaseRunnable(
    context: Context,
    database: ContextualDatabase,
    private val oldKeyDerivation: KdfEngine,
    private val newKeyDerivation: KdfEngine,
    save: Boolean,
    challengeOperation: ChallengeRequest.ChallengeOperation,
    challengeResponseRetriever: (ChallengeRequest) -> ByteArray,
    progressTaskUpdater: ProgressTaskUpdater?
) : SaveDatabaseRunnable(
    context = context,
    database = database,
    save = save,
    mainCredential = null,
    challengeOperation = challengeOperation,
    challengeResponseRetriever = challengeResponseRetriever,
    databaseCopyUri = null,
    dataModified = !save,
    progressTaskUpdater = progressTaskUpdater
) {
    override fun onActionRun() {
        // Assigned deserialized new Kdf object
        database.kdfEngine = newKeyDerivation
        if (newKeyDerivation.isDefault()) {
            val masterKey = database.masterKey
            // Randomize parameters (like seed or salt) if they are missing
            if (newKeyDerivation.getSeed() == null) {
                newKeyDerivation.randomize()
            }
            progressTaskUpdater?.benchmarking()
            newKeyDerivation.optimizeByBenchmark(
                masterKey = masterKey,
                limits = context.getKdfLimits()
            )
        }
        super.onActionRun()
    }

    override fun onFinishRun() {
        super.onFinishRun()
        if (!result.isSuccess) {
            try {
                database.kdfEngine = oldKeyDerivation
            } catch (e: Exception) {
                setError(e)
            }
        }
    }
}
