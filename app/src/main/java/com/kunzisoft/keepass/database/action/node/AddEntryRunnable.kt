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

import android.content.Context
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.Node

class AddEntryRunnable constructor(
        context: Context,
        database: Database,
        private val mNewEntry: Entry,
        private val mParent: Group,
        save: Boolean,
        afterActionNodesFinish: AfterActionNodesFinish?)
    : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save) {

    override fun nodeAction() {
        mNewEntry.touch(modified = true, touchParents = true)
        mParent.touch(modified = true, touchParents = true)
        database.addEntryTo(mNewEntry, mParent)
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            mNewEntry.parent?.let {
                database.removeEntryFrom(mNewEntry, it)
            }
        }

        val oldNodesReturn = ArrayList<Node>()
        val newNodesReturn = ArrayList<Node>()
        newNodesReturn.add(mNewEntry)
        return ActionNodesValues(oldNodesReturn, newNodesReturn)
    }
}
