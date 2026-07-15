/*
 * Copyright 2024 Jeremy Jamet / Kunzisoft.
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
 */

package com.kunzisoft.keepass.database.action.node

import android.content.Context
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.CloseableIterator

class ImportRunnable(
    context: Context,
    database: ContextualDatabase,
    private val mEntrySource: CloseableIterator<Entry>,
    private val mParent: Group,
    save: Boolean,
    afterActionNodesFinish: AfterActionNodesFinish?,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save, challengeResponseRetriever) {

    private val mImportedEntries = mutableListOf<Entry>()

    override fun nodeAction() {
        mParent.touch(modified = true, touchParents = true)
        for (entry in mEntrySource) {
            entry.touch(modified = true, touchParents = true)
            database.addEntryTo(entry, mParent)
            mImportedEntries.add(entry)
        }
        mEntrySource.close()
    }

    override fun nodeFinish(): ActionNodesValues {
        return ActionNodesValues(listOf(), mImportedEntries)
    }
}
