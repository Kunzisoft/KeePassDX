/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.action.node

import androidx.fragment.app.FragmentActivity
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.*

class MoveNodesRunnable constructor(
        context: FragmentActivity,
        database: Database,
        private val mNodesToMove: List<NodeVersioned>,
        private val mNewParent: GroupVersioned,
        afterAddNodeRunnable: AfterActionNodeFinishRunnable?,
        save: Boolean)
    : ActionNodeDatabaseRunnable(context, database, afterAddNodeRunnable, save) {

    private var mOldParent: GroupVersioned? = null

    override fun nodeAction() {

        mNodesToMove.forEach { nodeToMove ->
            // Move node in new parent
            mOldParent = nodeToMove.parent

            when (nodeToMove.type) {
                Type.GROUP -> {
                    val groupToMove = nodeToMove as GroupVersioned
                    // Move group in new parent if not in the current group
                    if (groupToMove != mNewParent
                            && !mNewParent.isContainedIn(groupToMove)) {
                        database.moveGroupTo(groupToMove, mNewParent)
                        finishRun(true)
                    } else {
                        // Only finish thread
                        throw Exception(context.getString(R.string.error_move_folder_in_itself))
                    }
                }
                Type.ENTRY -> {
                    val entryToMove = nodeToMove as EntryVersioned
                    // Move only if the parent change
                    if (mOldParent != mNewParent
                            // and root can contains entry
                            && (mNewParent != database.rootGroup || database.rootCanContainsEntry())) {
                        database.moveEntryTo(entryToMove, mNewParent)
                        finishRun(true)
                    } else {
                        // Only finish thread
                        throw Exception(context.getString(R.string.error_move_entry_here))
                    }
                }
            }

            nodeToMove.touch(modified = true, touchParents = true)
        }
    }

    override fun nodeFinish(result: Result): ActionNodeValues {
        if (!result.isSuccess) {
            try {
                mNodesToMove.forEach { nodeToMove ->
                    // If we fail to save, try to move in the first place
                    if (mOldParent != null) {
                        when (nodeToMove.type) {
                            Type.GROUP -> database.moveGroupTo(nodeToMove as GroupVersioned, mOldParent!!)
                            Type.ENTRY -> database.moveEntryTo(nodeToMove as EntryVersioned, mOldParent!!)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.i(TAG, "Unable to replace the node")
            }
        }
        return ActionNodeValues(result, ArrayList(), mNodesToMove)
    }

    companion object {
        private val TAG = MoveNodesRunnable::class.java.name
    }
}
