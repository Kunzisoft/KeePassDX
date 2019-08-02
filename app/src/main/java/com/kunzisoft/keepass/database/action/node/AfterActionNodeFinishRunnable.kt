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

import com.kunzisoft.keepass.database.element.NodeVersioned
import com.kunzisoft.keepass.tasks.ActionRunnable

/**
 * Callback method who return the node(s) modified after an action
 * - Add : @param oldNode NULL, @param newNode CreatedNode
 * - Copy : @param oldNode NodeToCopy, @param newNode NodeCopied
 * - Delete : @param oldNode NodeToDelete, @param NULL
 * - Move : @param oldNode NULL, @param NodeToMove
 * - Update : @param oldNode NodeToUpdate, @param NodeUpdated
 */
data class ActionNodeValues(val result: ActionRunnable.Result, val oldNode: NodeVersioned?, val newNode: NodeVersioned?)

abstract class AfterActionNodeFinishRunnable {
    abstract fun onActionNodeFinish(actionNodeValues: ActionNodeValues)
}
