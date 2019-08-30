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
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.widget.Button
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.UriUtil

class BrowserDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            // Get the layout inflater
            val root = activity.layoutInflater.inflate(R.layout.fragment_browser_install, null)
            builder.setView(root)
                    .setNegativeButton(R.string.cancel) { _, _ -> }

            val textDescription = root.findViewById<TextView>(R.id.file_manager_install_description)
            textDescription.text = getString(R.string.file_manager_install_description)

            val market = root.findViewById<Button>(R.id.file_manager_install_play_store)
            market.setOnClickListener {
                UriUtil.gotoUrl(context!!, R.string.filemanager_play_store)
                dismiss()
            }

            val web = root.findViewById<Button>(R.id.file_manager_install_f_droid)
            web.setOnClickListener {
                UriUtil.gotoUrl(context!!, R.string.filemanager_f_droid)
                dismiss()
            }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }
}
