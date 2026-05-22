/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.action.history

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.action.node.UpdateEntryRunnable
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.tasks.ActionRunnable
import java.util.UUID

class RestoreEntryHistoryDatabaseRunnable (
    private val context: Context,
    private val database: ContextualDatabase,
    entryId: NodeId<UUID>,
    private val entryHistoryPosition: Int,
    private val save: Boolean,
    private val challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionRunnable() {

    private var updateEntryRunnable: UpdateEntryRunnable? = null
    private var mMainEntry: Entry? = null

    init {
        database.getEntryById(entryId)?.let { entry ->
            mMainEntry = entry
        }
    }

    override fun onStartRun() {
        try {
            mMainEntry?.let { mainEntry ->
                val historyToRestore = database.getEntryInfoFrom(
                    entry = mainEntry.getHistory()[entryHistoryPosition],
                    raw = true
                )
                // Update the entry with the fresh formatted entry to restore
                updateEntryRunnable = UpdateEntryRunnable(
                    context = context,
                    database = database,
                    newEntry = historyToRestore,
                    save = save,
                    afterActionNodesFinish = null,
                    challengeResponseRetriever = challengeResponseRetriever
                )

                updateEntryRunnable?.onStartRun()
            } ?: setError("Entry to restore cannot be null")
        } catch (e: Exception) {
            setError(e)
        }
    }

    override fun onActionRun() {
        updateEntryRunnable?.onActionRun()
    }

    override fun onFinishRun() {
        updateEntryRunnable?.onFinishRun()
    }
}
