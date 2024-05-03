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
package com.kunzisoft.keepass.database.file.input

import android.util.Log
import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import com.kunzisoft.keepass.database.exception.DatabaseInputException
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import java.io.InputStream

abstract class DatabaseInput<D : DatabaseVersioned<*, *, *, *>> (protected var mDatabase: D) {

    private var startTimeKey = System.currentTimeMillis()
    private var startTimeContent = System.currentTimeMillis()

    /**
     * Load a versioned database file, return contents in a new DatabaseVersioned.
     */

    @Throws(DatabaseInputException::class)
    abstract fun openDatabase(databaseInputStream: InputStream,
                              progressTaskUpdater: ProgressTaskUpdater?,
                              assignMasterKey: (() -> Unit)): D

    protected fun startKeyTimer(progressTaskUpdater: ProgressTaskUpdater?) {
        progressTaskUpdater?.retrievingDatabaseKey()
        Log.d(TAG, "Start retrieving database key...")
        startTimeKey = System.currentTimeMillis()
    }

    protected fun stopKeyTimer() {
        Log.d(TAG, "Stop retrieving database key... ${System.currentTimeMillis() - startTimeKey} ms")
    }

    protected fun startContentTimer(progressTaskUpdater: ProgressTaskUpdater?) {
        progressTaskUpdater?.decryptingDatabase()
        Log.d(TAG, "Start decrypting database content...")
        startTimeContent = System.currentTimeMillis()
    }

    protected fun stopContentTimer() {
        Log.d(TAG, "Stop retrieving database content... ${System.currentTimeMillis() - startTimeContent} ms")
    }

    companion object {
        private val TAG = DatabaseInput::class.java.name
    }
}
