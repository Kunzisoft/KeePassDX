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

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager

import com.kunzisoft.keepass.R

class ReadOnlyDialog(context: Context) : AlertDialog(context) {

    override fun onCreate(savedInstanceState: Bundle) {
        val ctx = context
        var warning = ctx.getString(R.string.read_only_warning)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            warning = warning + "\n\n" + context.getString(R.string.read_only_kitkat_warning)
        }
        setMessage(warning)

        setButton(BUTTON_POSITIVE, ctx.getText(android.R.string.ok)) { _, _ -> dismiss() }
        setButton(BUTTON_NEGATIVE, ctx.getText(R.string.beta_dontask)) { _, _ ->
            val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
            val edit = prefs.edit()
            edit.putBoolean(ctx.getString(R.string.show_read_only_warning), false)
            edit.apply()
            dismiss()
        }

        super.onCreate(savedInstanceState)
    }
}
