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
package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.UriUtil.openUrl

class FileManagerDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            // Get the layout inflater
            val root = activity.layoutInflater.inflate(R.layout.fragment_browser_install, null)
            builder.setView(root)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }

            val textDescription = root.findViewById<TextView>(R.id.file_manager_install_description)
            textDescription.text = getString(R.string.file_manager_install_description)

            root.findViewById<Button>(R.id.file_manager_button).setOnClickListener {
                context?.openUrl(R.string.file_manager_explanation_url)
                dismiss()
            }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }
}
