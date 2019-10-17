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

import android.content.res.Resources
import android.os.Bundle
import android.view.View
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.settings.SettingsActivity
import com.kunzisoft.keepass.tasks.ActionRunnable

abstract class DatabaseSavePreferenceDialogFragmentCompat : InputPreferenceDialogFragmentCompat() {

    protected var database: Database? = null

    var actionInUIThreadAfterSaveDatabase: ActionRunnable? = null

    protected lateinit var settingsResources: Resources

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.database = Database.getInstance()
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        activity?.resources?.let { settingsResources = it }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            registerActionFinish()
            (activity as SettingsActivity?)?.progressDialogThread?.startDatabaseSave()
        }
    }

    fun registerActionFinish() {
        // TODO remove receiver
        // Register a database task receiver to stop loading dialog when service finish the task
        /*
        when (intent?.action) {
            DATABASE_STOP_TASK_ACTION -> {
                actionInUIThreadAfterSaveDatabase?.onFinishRun(result)
            }
        }
        */
    }

    companion object {

        private const val TAG = "DbSavePrefDialog"
    }
}
