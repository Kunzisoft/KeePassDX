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
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.action.SaveDatabaseRunnable
import com.kunzisoft.keepass.hardware.HardwareKey

abstract class ActionNodeDatabaseRunnable(
    context: Context,
    database: ContextualDatabase,
    private val afterActionNodesFinish: AfterActionNodesFinish?,
    save: Boolean,
    challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
) : SaveDatabaseRunnable(context, database, save, null, challengeResponseRetriever) {

    /**
     * Function do to a node action
     */
    abstract fun nodeAction()

    override fun onStartRun() {
        try {
            nodeAction()
        } catch (e: Exception) {
            setError(e)
        }
        super.onStartRun()
    }

    /**
     * Function do get the finish node action
     */
    abstract fun nodeFinish(): ActionNodesValues

    override fun onFinishRun() {
        super.onFinishRun()
        afterActionNodesFinish?.apply {
            onActionNodesFinish(result, nodeFinish())
        }
    }
}
