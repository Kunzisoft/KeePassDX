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
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.security.ProtectedString

class EntryCustomFieldDialogFragment: DialogFragment() {

    private var entryCustomFieldListener: EntryCustomFieldListener? = null

    private var newFieldLabel: TextView? = null
    private var newFieldValue: TextView? = null
    private var newFieldProtection: CompoundButton? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            entryCustomFieldListener = context as EntryCustomFieldListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(context.toString()
                    + " must implement " + EntryCustomFieldListener::class.java.name)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val root = activity.layoutInflater.inflate(R.layout.fragment_entry_new_field, null)
            newFieldLabel = root?.findViewById(R.id.entry_new_field_label)
            newFieldValue = root?.findViewById(R.id.entry_new_field_value)
            newFieldProtection = root?.findViewById(R.id.entry_new_field_protection)

            val builder = AlertDialog.Builder(activity)
            builder.setView(root)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        entryCustomFieldListener?.onNewCustomFieldCanceled(
                                newFieldLabel?.text.toString(),
                                ProtectedString(
                                        newFieldProtection?.isChecked == true,
                                        newFieldValue?.text.toString()
                                )
                        )
                    }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        // To prevent auto dismiss
        val d = dialog as AlertDialog?
        if (d != null) {
            val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE) as Button
            positiveButton.setOnClickListener {
                if (isValid()) {
                    entryCustomFieldListener?.onNewCustomFieldApproved(
                            newFieldLabel?.text.toString(),
                            ProtectedString(
                                    newFieldProtection?.isChecked == true,
                                    newFieldValue?.text.toString()
                            )
                    )
                    d.dismiss()
                }
            }
        }
    }

    private fun isValid(): Boolean {
        return if (newFieldLabel?.text?.toString()?.isNotEmpty() != true) {
            setError(R.string.error_string_key)
            false
        } else {
            setError(null)
            true
        }
    }

    fun setError(@StringRes errorId: Int?) {
        newFieldLabel?.error = if (errorId == null) null else {
            requireContext().getString(errorId)
        }
    }

    interface EntryCustomFieldListener {
        fun onNewCustomFieldApproved(label: String, name: ProtectedString)
        fun onNewCustomFieldCanceled(label: String, name: ProtectedString)
    }

    companion object {
        fun getInstance(): EntryCustomFieldDialogFragment {
            return EntryCustomFieldDialogFragment()
        }
    }
}
