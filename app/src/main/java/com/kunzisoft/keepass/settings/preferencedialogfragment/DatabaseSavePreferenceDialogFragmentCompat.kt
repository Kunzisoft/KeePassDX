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
package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.view.View
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.action.SaveDatabaseProgressDialogRunnable
import com.kunzisoft.keepass.tasks.ActionRunnable

abstract class DatabaseSavePreferenceDialogFragmentCompat : InputPreferenceDialogFragmentCompat() {

    protected var database: Database? = null

    var afterSaveDatabaseRunnable: ActionRunnable? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        this.database = App.getDB()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            activity?.let { notNullActivity ->
                database?.let { notNullDatabase ->
                    afterSaveDatabaseRunnable?.let { runnable ->
                        Thread(SaveDatabaseProgressDialogRunnable(
                                notNullActivity,
                                notNullDatabase,
                                { runnable },
                                true)).start() // TODO Read only
                    }
                }
            }
        }
    }
}
