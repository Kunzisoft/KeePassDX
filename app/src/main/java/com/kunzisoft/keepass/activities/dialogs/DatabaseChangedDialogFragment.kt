/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.SnapFileDatabaseInfo
import com.kunzisoft.keepass.utils.getParcelableCompat


class DatabaseChangedDialogFragment : DatabaseDialogFragment() {

    var actionDatabaseListener: ActionDatabaseChangedListener? = null

    override fun onPause() {
        super.onPause()
        actionDatabaseListener = null
        this.dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->

            val oldSnapFileDatabaseInfo: SnapFileDatabaseInfo? = arguments?.getParcelableCompat(OLD_FILE_DATABASE_INFO)
            val newSnapFileDatabaseInfo: SnapFileDatabaseInfo? = arguments?.getParcelableCompat(NEW_FILE_DATABASE_INFO)
            val readOnlyDatabase: Boolean = arguments?.getBoolean(READ_ONLY_DATABASE) ?: true

            if (oldSnapFileDatabaseInfo != null && newSnapFileDatabaseInfo != null) {
                // Use the Builder class for convenient dialog construction
                val builder = AlertDialog.Builder(activity)

                val stringBuilder = SpannableStringBuilder()
                if (newSnapFileDatabaseInfo.exists) {
                    stringBuilder.append(getString(R.string.warning_database_info_changed))
                    stringBuilder.append("\n\n" +oldSnapFileDatabaseInfo.toString(activity)
                            + "\n→\n" +
                            newSnapFileDatabaseInfo.toString(activity) + "\n\n")
                    stringBuilder.append(getString(
                        if (readOnlyDatabase) {
                            R.string.warning_database_info_changed_options_read_only
                        } else {
                            R.string.warning_database_info_changed_options
                        }
                    ))
                } else {
                    stringBuilder.append(getString(R.string.warning_database_revoked))
                }
                builder.setMessage(stringBuilder)
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    actionDatabaseListener?.validateDatabaseChanged()
                }
                return builder.create()
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    interface ActionDatabaseChangedListener {
        fun validateDatabaseChanged()
    }

    companion object {

        const val DATABASE_CHANGED_DIALOG_TAG = "databaseChangedDialogFragment"
        private const val OLD_FILE_DATABASE_INFO = "OLD_FILE_DATABASE_INFO"
        private const val NEW_FILE_DATABASE_INFO = "NEW_FILE_DATABASE_INFO"
        private const val READ_ONLY_DATABASE = "READ_ONLY_DATABASE"

        fun getInstance(oldSnapFileDatabaseInfo: SnapFileDatabaseInfo,
                        newSnapFileDatabaseInfo: SnapFileDatabaseInfo,
                        readOnly: Boolean
        )
        : DatabaseChangedDialogFragment {
            val fragment = DatabaseChangedDialogFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(OLD_FILE_DATABASE_INFO, oldSnapFileDatabaseInfo)
                putParcelable(NEW_FILE_DATABASE_INFO, newSnapFileDatabaseInfo)
                putBoolean(READ_ONLY_DATABASE, readOnly)
            }
            return fragment
        }
    }
}
