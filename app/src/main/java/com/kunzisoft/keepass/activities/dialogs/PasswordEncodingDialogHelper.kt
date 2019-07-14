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
import android.content.DialogInterface

import com.kunzisoft.keepass.R

class PasswordEncodingDialogHelper {
    private var dialog: AlertDialog? = null

    @JvmOverloads
    fun show(ctx: Context, onclick: DialogInterface.OnClickListener, showCancel: Boolean = false) {
        val builder = AlertDialog.Builder(ctx)
        builder.setMessage(R.string.warning_password_encoding).setTitle(R.string.warning)
        builder.setPositiveButton(android.R.string.ok, onclick)

        if (showCancel) {
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
        }

        dialog = builder.create()
        dialog?.show()
    }
}
