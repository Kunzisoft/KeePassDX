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
package com.kunzisoft.keepass.settings.preferencedialogfragment.adapter

import android.os.Bundle
import android.view.View
import com.kunzisoft.keepass.settings.preferencedialogfragment.DatabaseSavePreferenceDialogFragmentCompat
import com.kunzisoft.keepass.tasks.ActionRunnable

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

                actionInUIThreadAfterSaveDatabase = AfterDefaultUsernameSave(newDefaultUsername, oldDefaultUsername)
            }
        }

        super.onDialogClosed(positiveResult)
    }

    private inner class AfterDefaultUsernameSave(private val mNewDefaultUsername: String,
                                                 private val mOldDefaultUsername: String)
        : ActionRunnable() {

        override fun onFinishRun(result: Result) {
            val defaultUsernameToShow =
                    if (result.isSuccess) {
                        mNewDefaultUsername
                    } else {
                        database?.defaultUsername = mOldDefaultUsername
                        mOldDefaultUsername
                    }
            preference.summary = defaultUsernameToShow
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
