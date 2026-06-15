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
package com.kunzisoft.keepass.database.action.node

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.hardware.HardwareKey

class TouchEntryRunnable(
    context: Context,
    database: ContextualDatabase,
    oldEntryId: EntryId,
    afterActionNodesFinish: AfterActionNodesFinish?,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionNodeDatabaseRunnable(
    context,
    database,
    afterActionNodesFinish,
    save = false,
    challengeResponseRetriever,
    dataModified = false
) {

    private var mEntry: Entry? = null

    init {
        database.getEntryById(oldEntryId)?.let { entry ->
            mEntry = entry
        }
    }

    override fun nodeAction() {
        mEntry?.let { entry ->
            entry.touch(modified = false, touchParents = true)
            database.updateEntry(entry = entry)
        }
    }

    override fun nodeFinish(): ActionNodesValues {
        return ActionNodesValues(
            oldEntriesIds = mEntry?.nodeId?.let { listOf(it) }
        )
    }
}
