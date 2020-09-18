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

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R

class PasswordEncodingDialogFragment : DialogFragment() {

    private var mListener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as Listener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString()
                    + " must implement " + Listener::class.java.name)
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val databaseUri: Uri? = savedInstanceState?.getParcelable(DATABASE_URI_KEY)
        val masterPasswordChecked: Boolean = savedInstanceState?.getBoolean(MASTER_PASSWORD_CHECKED_KEY) ?: false
        val masterPassword: String? = savedInstanceState?.getString(MASTER_PASSWORD_KEY)
        val keyFileChecked: Boolean = savedInstanceState?.getBoolean(KEY_FILE_CHECKED_KEY) ?: false
        val keyFile: Uri? = savedInstanceState?.getParcelable(KEY_FILE_URI_KEY)

        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(activity.getString(R.string.warning_password_encoding)).setTitle(R.string.warning)
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                mListener?.onPasswordEncodingValidateListener(
                        databaseUri,
                        masterPasswordChecked,
                        masterPassword,
                        keyFileChecked,
                        keyFile
                )
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    interface Listener {
        fun onPasswordEncodingValidateListener(databaseUri: Uri?,
                                               masterPasswordChecked: Boolean,
                                               masterPassword: String?,
                                               keyFileChecked: Boolean,
                                               keyFile: Uri?)
    }

    companion object {

        private const val DATABASE_URI_KEY = "DATABASE_URI_KEY"
        private const val MASTER_PASSWORD_CHECKED_KEY = "MASTER_PASSWORD_CHECKED_KEY"
        private const val MASTER_PASSWORD_KEY = "MASTER_PASSWORD_KEY"
        private const val KEY_FILE_CHECKED_KEY = "KEY_FILE_CHECKED_KEY"
        private const val KEY_FILE_URI_KEY = "KEY_FILE_URI_KEY"

        fun getInstance(databaseUri: Uri,
                        masterPasswordChecked: Boolean,
                        masterPassword: String?,
                        keyFileChecked: Boolean,
                        keyFile: Uri?): SortDialogFragment {
            val fragment = SortDialogFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(DATABASE_URI_KEY, databaseUri)
                putBoolean(MASTER_PASSWORD_CHECKED_KEY, masterPasswordChecked)
                putString(MASTER_PASSWORD_KEY, masterPassword)
                putBoolean(KEY_FILE_CHECKED_KEY, keyFileChecked)
                putParcelable(KEY_FILE_URI_KEY, keyFile)
            }
            return fragment
        }
    }
}
