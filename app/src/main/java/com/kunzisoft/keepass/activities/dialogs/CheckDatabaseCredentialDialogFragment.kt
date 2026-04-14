/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
import android.text.InputFilter
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.utils.UriUtil.openUrl
import com.kunzisoft.keepass.viewmodels.UserVerificationViewModel


class CheckDatabaseCredentialDialogFragment : DatabaseDialogFragment() {

    private val userVerificationViewModel: UserVerificationViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            val inflater = activity.layoutInflater
            val rootView = inflater.inflate(R.layout.fragment_check_database_credential, null)
            val editText = rootView.findViewById<TextView>(R.id.setup_check_password_edit_text)
            editText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(
                MasterCredential.CHECK_KEY_PASSWORD_LENGTH)
            )
            editText.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                    validate(editText.text.toString())
                    true
                } else {
                    false
                }
            }
            builder.setView(rootView)
                    .setPositiveButton(R.string.check) { _, _ ->
                        validate(editText.text.toString())
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        userVerificationViewModel.onUserVerificationFailed()
                        dismiss()
                    }
            rootView.findViewById<View>(R.id.user_verification_information)?.setOnClickListener {
                activity.openUrl(R.string.user_verification_explanation_url)
            }
            return builder.create()
        }

        return super.onCreateDialog(savedInstanceState)
    }

    private fun validate(password: String) {
        userVerificationViewModel.checkMainCredential(password)
        dismiss()
    }

    companion object {

        fun getInstance(): CheckDatabaseCredentialDialogFragment {
            val fragment = CheckDatabaseCredentialDialogFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
