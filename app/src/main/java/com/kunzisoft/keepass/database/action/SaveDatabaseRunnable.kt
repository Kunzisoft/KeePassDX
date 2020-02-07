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
package com.kunzisoft.keepass.database.action

import android.content.Context
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.exception.DatabaseException
import com.kunzisoft.keepass.tasks.ActionRunnable

open class SaveDatabaseRunnable(protected var context: Context,
                                protected var database: Database,
                                private var saveDatabase: Boolean)
    : ActionRunnable() {

    var mAfterSaveDatabase: ((Result) -> Unit)? = null

    override fun onStartRun() {}

    override fun onActionRun() {
        if (saveDatabase && result.isSuccess) {
            try {
                database.saveData(context.contentResolver)
            } catch (e: DatabaseException) {
                setError(e)
            }
        }
    }

    override fun onFinishRun() {
        // Need to call super.onFinishRun() in child class
        mAfterSaveDatabase?.invoke(result)
    }
}
