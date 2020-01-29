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
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.exception.CopyEntryDatabaseException
import com.kunzisoft.keepass.database.exception.CopyGroupDatabaseException

class CopyNodesRunnable constructor(
        context: Context,
        database: Database,
        private val mNodesToCopy: List<Node>,
        private val mNewParent: Group,
        save: Boolean,
        afterActionNodesFinish: AfterActionNodesFinish?)
    : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save) {

    private var mEntriesCopied = ArrayList<Entry>()

    override fun nodeAction() {

        foreachNode@ for(currentNode in mNodesToCopy) {
            when (currentNode.type) {
                Type.GROUP -> {
                    Log.e(TAG, "Copy not allowed for group")// Only finish thread
                    setError(CopyGroupDatabaseException())
                    break@foreachNode
                }
                Type.ENTRY -> {
                    // Root can contains entry
                    if (mNewParent != database.rootGroup || database.rootCanContainsEntry()) {
                        // Update entry with new values
                        mNewParent.touch(modified = false, touchParents = true)

                        val entryCopied = database.copyEntryTo(currentNode as Entry, mNewParent)
                        if (entryCopied != null) {
                            entryCopied.touch(modified = true, touchParents = true)
                            mEntriesCopied.add(entryCopied)
                        } else {
                            Log.e(TAG, "Unable to create a copy of the entry")
                            setError(CopyEntryDatabaseException())
                            break@foreachNode
                        }
                    } else {
                        // Only finish thread
                        setError(CopyEntryDatabaseException())
                        break@foreachNode
                    }
                }
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
