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
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node

class AddGroupRunnable constructor(
        context: Context,
        database: Database,
        private val mNewGroup: Group,
        private val mParent: Group,
        save: Boolean,
        afterActionNodesFinish: AfterActionNodesFinish?)
    : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save) {

    override fun nodeAction() {
        mNewGroup.touch(modified = true, touchParents = true)
        mParent.touch(modified = true, touchParents = true)
        database.addGroupTo(mNewGroup, mParent)
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            database.removeGroupFrom(mNewGroup, mParent)
        }

        val oldNodesReturn = ArrayList<Node>()
        val newNodesReturn = ArrayList<Node>()
        newNodesReturn.add(mNewGroup)
        return ActionNodesValues(oldNodesReturn, newNodesReturn)
    }
}
