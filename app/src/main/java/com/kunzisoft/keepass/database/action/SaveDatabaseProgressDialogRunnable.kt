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

import android.support.v4.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.Database
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater

open class SaveDatabaseProgressDialogRunnable(var contextDatabase: FragmentActivity,
                                              database: Database,
                                              private val actionRunnable: ((ProgressTaskUpdater)-> ActionRunnable)?,
                                              save: Boolean):
        SaveDatabaseRunnable(contextDatabase, database, null, save) {

    override fun run() {
        // Show the dialog
        val progressTaskUpdater : ProgressTaskUpdater = ProgressTaskDialogFragment.start(
                contextDatabase.supportFragmentManager,
                R.string.saving_database)

        // Do the action if defined
        actionRunnable?.invoke(progressTaskUpdater)?.run()

        // Save the database
        super.run()

        // Call the finish function to close the dialog
        finishRun(true)
    }

    override fun onFinishRun(isSuccess: Boolean, message: String?) {
        super.onFinishRun(isSuccess, message)

        // Remove the progress task
        ProgressTaskDialogFragment.stop(contextDatabase)
    }
}