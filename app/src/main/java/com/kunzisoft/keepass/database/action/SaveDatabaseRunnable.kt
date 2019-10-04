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
package com.kunzisoft.keepass.database.action

import android.content.Context
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.tasks.ActionRunnable
import java.io.IOException

abstract class SaveDatabaseRunnable(protected var context: Context,
                                protected var database: Database,
                                private val save: Boolean,
                                nestedAction: ActionRunnable? = null) : ActionRunnable(nestedAction) {

    // TODO Service to prevent background thread kill
    override fun run() {
        if (save) {
            try {
                database.saveData(context.contentResolver)
            } catch (e: IOException) {
                finishRun(false, e.message)
            } catch (e: DatabaseOutputException) {
                finishRun(false, e.message)
            }
        }

        // Need to call super.run() in child class
    }

    override fun onFinishRun(result: Result) {
        // Need to call super.onFinishRun(result) in child class
    }
}

class SaveDatabaseActionRunnable(context: Context,
                                 database: Database,
                                 save: Boolean,
                                 nestedAction: ActionRunnable? = null)
    : SaveDatabaseRunnable(context, database, save, nestedAction) {

    override fun run() {
        super.run()
        finishRun(true)
    }
}
