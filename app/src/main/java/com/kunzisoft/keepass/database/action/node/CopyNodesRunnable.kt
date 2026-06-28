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
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.database.exception.CopyEntryDatabaseException
import com.kunzisoft.keepass.database.exception.CopyGroupDatabaseException
import com.kunzisoft.keepass.database.exception.MissingParentDatabaseException
import com.kunzisoft.keepass.hardware.HardwareKey

class CopyNodesRunnable(
    context: Context,
    database: ContextualDatabase,
    newParentId: GroupId,
    val groupsIdsToCopy: List<GroupId>,
    val entriesIdsToCopy: List<EntryId>,
    save: Boolean,
    afterActionNodesFinish: AfterActionNodesFinish?,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionNodeDatabaseRunnable(
    context,
    database,
    afterActionNodesFinish,
    save,
    challengeResponseRetriever
) {

    private var mGroupsCopied = mutableListOf<Group>()
    private var mEntriesCopied = mutableListOf<Entry>()
    private var mNewParent: Group? = null
    private var mGroupsToCopy: List<Group> = listOf()
    private var mEntriesToCopy: List<Entry> = listOf()

    init {
        database.getGroupById(newParentId)?.let { newParent ->
            mNewParent = newParent
        }
        mGroupsToCopy = database.getGroupsByIds(groupsIdsToCopy)
        mEntriesToCopy = database.getEntriesByIds(entriesIdsToCopy)
    }

    override fun nodeAction() {
        val newParent = mNewParent ?: run {
            setError(MissingParentDatabaseException())
            return
        }

        for (currentNode in mGroupsToCopy) {
            try {
                val groupCopied = database.copyGroupTo(currentNode, newParent)
                mGroupsCopied.add(groupCopied)
            } catch (_: Exception) {
                setError(CopyGroupDatabaseException())
                return
            }
        }

        for (currentNode in mEntriesToCopy) {
            // Root can contains entry
            if (newParent != database.rootGroup || database.rootCanContainsEntry()) {
                // Update entry with new values
                newParent.touch(modified = false, touchParents = true)
                try {
                    val entryCopied = database.copyEntryTo(
                        entryToCopy = currentNode,
                        newParent = newParent
                    )
                    entryCopied.touch(modified = true, touchParents = true)
                    mEntriesCopied.add(entryCopied)
                } catch (_: Exception) {
                    setError(CopyEntryDatabaseException())
                    return
                }
            } else {
                // Only finish thread
                setError(CopyEntryDatabaseException())
                return
            }
        }
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            // If we fail to save, try to delete the copy
            mEntriesCopied.forEach {
                try {
                    database.deleteEntry(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to delete the copied entry", e)
                }
            }
            // Restore groups in reverse order of creation
            mGroupsCopied.reversed().forEach {
                try {
                    database.deleteGroup(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to delete the copied group", e)
                }
            }
        }
        return ActionNodesValues(
            oldGroupsIds = groupsIdsToCopy,
            oldEntriesIds = entriesIdsToCopy,
            newGroupsIds = mGroupsCopied.map { it.nodeId },
            newEntriesIds = mEntriesCopied.map { it.nodeId }
        )
    }

    companion object {
        private val TAG = CopyNodesRunnable::class.java.name
    }
}
