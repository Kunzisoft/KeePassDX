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

import android.os.Bundle
import android.view.View
import com.kunzisoft.keepass.tasks.ActionRunnable

class DatabaseNamePreferenceDialogFragmentCompat : InputDatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        inputText = database?.name ?: ""
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            database?.let { database ->
                val newName = inputText
                val oldName = database.name
                database.assignName(newName)

                actionInUIThreadAfterSaveDatabase = AfterNameSave(newName, oldName)
            }
        }

        super.onDialogClosed(positiveResult)
    }

    private inner class AfterNameSave(private val mNewName: String,
                                      private val mOldName: String)
        : ActionRunnable() {

        override fun onFinishRun(result: Result) {
            val nameToShow = mNewName
            if (!result.isSuccess) {
                database?.assignName(mOldName)
            }
            preference.summary = nameToShow
        }
    }

    companion object {

        fun newInstance(
                key: String): DatabaseNamePreferenceDialogFragmentCompat {
            val fragment = DatabaseNamePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
