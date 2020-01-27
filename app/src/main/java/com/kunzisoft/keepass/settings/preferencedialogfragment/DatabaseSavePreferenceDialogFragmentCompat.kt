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
package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.content.Context
import android.os.Bundle
import com.kunzisoft.keepass.database.action.ProgressDialogThread
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.settings.SettingsActivity

abstract class DatabaseSavePreferenceDialogFragmentCompat : InputPreferenceDialogFragmentCompat() {

    protected var database: Database? = null
    protected var mDatabaseAutoSaveEnable = true
    protected var mProgressDialogThread: ProgressDialogThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.database = Database.getInstance()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Attach dialog thread to start action
        if (context is SettingsActivity) {
            mProgressDialogThread = context.mProgressDialogThread
        }

        this.mDatabaseAutoSaveEnable = PreferencesUtil.isAutoSaveDatabaseEnabled(context)
    }

    companion object {
        private const val TAG = "DbSavePrefDialog"
    }
}
