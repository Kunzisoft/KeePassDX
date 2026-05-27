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
package com.kunzisoft.keepass.database.action.node

import android.content.Context
import android.util.Log
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.model.EntryInfo

class AddEntryRunnable(
    context: Context,
    database: ContextualDatabase,
    parentId: GroupId,
    newEntry: EntryInfo,
    save: Boolean,
    afterActionNodesFinish: AfterActionNodesFinish?,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save, challengeResponseRetriever) {

    private var mParent: Group? = null
    private var mNewEntry: Entry? = null

    init {
        database.getGroupById(parentId)?.let { parent ->
            mParent = parent
        }
        if (mParent == null) {
            Log.w(TAG, "Unable to retrieve the parent to create the entry")
            mParent = database.rootGroup
        }

        // Create the new entry
        mNewEntry = database.createEntry(newEntry)?.apply {
            database.removeTempAttachmentsNotUsed(this)
        }
    }

    override fun nodeAction() {
        mNewEntry?.let { newEntry ->
            newEntry.touch(modified = true, touchParents = true)
            mParent?.let { parent ->
                parent.touch(modified = true, touchParents = true)
                database.addEntryTo(newEntry, parent)
            }
        }
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            mNewEntry?.let { newEntry ->
                newEntry.parent?.let {
                    database.removeEntryFrom(newEntry, it)
                }
            }
        }
        return ActionNodesValues(
            newEntriesIds = mNewEntry?.nodeId?.let { listOf(it) }
        )
    }

    companion object {
        private val TAG = AddEntryRunnable::class.simpleName
    }
}
