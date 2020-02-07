/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.action.history

import android.content.Context
import com.kunzisoft.keepass.database.action.SaveDatabaseRunnable
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry

class DeleteEntryHistoryDatabaseRunnable (
        context: Context,
        database: Database,
        private val mainEntry: Entry,
        private val entryHistoryPosition: Int,
        saveDatabase: Boolean)
    : SaveDatabaseRunnable(context, database, saveDatabase) {

    override fun onStartRun() {
        try {
            mainEntry.removeEntryFromHistory(entryHistoryPosition)
        } catch (e: Exception) {
            setError(e)
        }

        super.onStartRun()
    }
}
