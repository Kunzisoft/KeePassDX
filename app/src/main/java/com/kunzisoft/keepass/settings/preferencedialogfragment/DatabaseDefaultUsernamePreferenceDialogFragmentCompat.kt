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

import android.os.Bundle
import android.view.View

class DatabaseDefaultUsernamePreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        inputText = database?.defaultUsername?: ""
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        database?.let { database ->
            if (positiveResult) {
                val newDefaultUsername = inputText
                val oldDefaultUsername = database.defaultUsername
                database.defaultUsername = newDefaultUsername
                mProgressDialogThread?.startDatabaseSaveDefaultUsername(oldDefaultUsername, newDefaultUsername, mDatabaseAutoSaveEnable)
            }
        }
    }

    companion object {

        fun newInstance(key: String): DatabaseDefaultUsernamePreferenceDialogFragmentCompat {
            val fragment = DatabaseDefaultUsernamePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
