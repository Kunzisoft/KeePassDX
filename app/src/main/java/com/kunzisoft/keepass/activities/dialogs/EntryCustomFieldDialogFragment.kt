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
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R


class EntryCustomFieldDialogFragment: DialogFragment() {

    private var entryCustomFieldListener: EntryCustomFieldListener? = null

    private var newFieldLabelContainer: TextInputLayout? = null
    private var newFieldLabel: TextView? = null

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
            newFieldLabelContainer = root?.findViewById(R.id.new_field_label_container)
            newFieldLabel = root?.findViewById(R.id.new_field_label)

            val builder = AlertDialog.Builder(activity)
            builder.setView(root)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        entryCustomFieldListener?.onNewCustomFieldCanceled(
                                newFieldLabel?.text.toString()
                        )
                    }
            val dialogCreated = builder.create()

            newFieldLabel?.requestFocus()
            newFieldLabel?.imeOptions = EditorInfo.IME_ACTION_DONE
            newFieldLabel?.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    approveIfValid()
                }
                false
            }

            dialogCreated.window?.setSoftInputMode(SOFT_INPUT_STATE_VISIBLE)
            return dialogCreated

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
                approveIfValid()
            }
        }
    }

    private fun approveIfValid() {
        if (isValid()) {
            entryCustomFieldListener?.onNewCustomFieldApproved(
                    newFieldLabel?.text.toString()
            )
            (dialog as AlertDialog?)?.dismiss()
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
        newFieldLabelContainer?.error = if (errorId == null) null else {
            requireContext().getString(errorId)
        }
    }

    interface EntryCustomFieldListener {
        fun onNewCustomFieldApproved(label: String)
        fun onNewCustomFieldCanceled(label: String)
    }

    companion object {
        fun getInstance(): EntryCustomFieldDialogFragment {
            return EntryCustomFieldDialogFragment()
        }
    }
}
