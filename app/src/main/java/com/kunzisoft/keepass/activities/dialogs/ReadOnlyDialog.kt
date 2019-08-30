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
package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R

class ReadOnlyDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            // Use the Builder class for convenient dialog construction
            val builder = androidx.appcompat.app.AlertDialog.Builder(activity)

            var warning = getString(R.string.read_only_warning)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                warning = warning + "\n\n" + getString(R.string.read_only_kitkat_warning)
            }
            builder.setMessage(warning)

            builder.setPositiveButton(getString(android.R.string.ok)) { _, _ -> dismiss() }
            builder.setNegativeButton(getString(R.string.beta_dontask)) { _, _ ->
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val edit = prefs.edit()
                edit.putBoolean(getString(R.string.show_read_only_warning), false)
                edit.apply()
                dismiss()
            }

            // Create the AlertDialog object and return it
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }
}
