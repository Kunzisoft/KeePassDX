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
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.exception.CopyEntryDatabaseException
import com.kunzisoft.keepass.database.exception.CopyGroupDatabaseException
import com.kunzisoft.keepass.database.exception.MissingParentDatabaseException
import com.kunzisoft.keepass.hardware.HardwareKey
import java.util.UUID

class CopyNodesRunnable(
    context: Context,
    database: ContextualDatabase,
    newParentId: NodeId<*>,
    groupsIdsToCopy: List<NodeId<*>>,
    entriesIdsToCopy: List<NodeId<UUID>>,
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

    private var mEntriesCopied = mutableListOf<Entry>()
    private var mNewParent: Group? = null
    private var mNodesToCopy: List<Node> = listOf()

    init {
        database.getGroupById(newParentId)?.let { newParent ->
            mNewParent = newParent
        }
        mNodesToCopy = getListNodesFromBundle(database, groupsIdsToCopy, entriesIdsToCopy)
    }

    override fun nodeAction() {
        mNewParent?.let { newParent ->
            foreachNode@ for (currentNode in mNodesToCopy) {
                when (currentNode.type) {
                    Type.GROUP -> {
                        setError(CopyGroupDatabaseException())
                        break@foreachNode
                    }

                    Type.ENTRY -> {
                        // Root can contains entry
                        if (newParent != database.rootGroup || database.rootCanContainsEntry()) {
                            // Update entry with new values
                            newParent.touch(modified = false, touchParents = true)
                            val entryCopied = database.copyEntryTo(
                                entryToCopy = currentNode as Entry,
                                newParent = newParent
                            )
                            entryCopied.touch(modified = true, touchParents = true)
                            mEntriesCopied.add(entryCopied)
                        } else {
                            // Only finish thread
                            setError(CopyEntryDatabaseException())
                            break@foreachNode
                        }
                    }
                }
            }
        } ?: setError(MissingParentDatabaseException())
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            // If we fail to save, try to delete the copy
            mEntriesCopied.forEach {
                try {
                    database.deleteEntry(it)
                } catch (_: Exception) {
                    Log.i(TAG, "Unable to delete the copied entry")
                }
            }
        }
        return ActionNodesValues(mNodesToCopy, mEntriesCopied)
    }

    companion object {
        private val TAG = CopyNodesRunnable::class.java.name
    }
}
