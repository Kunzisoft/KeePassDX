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
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.tasks.ActionRunnable

class RestoreEntryHistoryDatabaseRunnable (
    private val context: Context,
    private val database: ContextualDatabase,
    private val mainEntry: Entry,
    private val entryHistoryPosition: Int,
    private val saveDatabase: Boolean,
    private val challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionRunnable() {

    private var updateEntryRunnable: UpdateEntryRunnable? = null

    override fun onStartRun() {
        try {
            val historyToRestore = Entry(mainEntry.getHistory()[entryHistoryPosition])
            // Copy history of main entry in the restore entry
            mainEntry.getHistory().forEach {
                historyToRestore.addEntryToHistory(it)
            }
            // Update the entry with the fresh formatted entry to restore
            updateEntryRunnable = UpdateEntryRunnable(
                context,
                database,
                mainEntry,
                historyToRestore,
                saveDatabase,
                null,
                challengeResponseRetriever
            )

            updateEntryRunnable?.onStartRun()

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
