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
import android.view.View
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.utils.getParcelableCompat


class EntryCustomFieldDialogFragment: DatabaseDialogFragment() {

    private var oldField: Field? = null

    private var entryCustomFieldListener: EntryCustomFieldListener? = null

    private var customFieldLabelContainer: TextInputLayout? = null
    private var customFieldLabel: TextView? = null
    private var customFieldDeleteButton: ImageView? = null
    private var customFieldProtectionButton: CompoundButton? = null

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

    override fun onDetach() {
        entryCustomFieldListener = null
        super.onDetach()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val root = activity.layoutInflater.inflate(R.layout.fragment_entry_new_field, null)
            customFieldLabelContainer = root?.findViewById(R.id.entry_custom_field_label_container)
            customFieldLabel = root?.findViewById(R.id.entry_custom_field_label)
            customFieldDeleteButton = root?.findViewById(R.id.entry_custom_field_delete)
            customFieldProtectionButton = root?.findViewById(R.id.entry_custom_field_protection)

            oldField = arguments?.getParcelableCompat(KEY_FIELD)
            oldField?.let { oldCustomField ->
                customFieldLabel?.text = oldCustomField.name
                customFieldProtectionButton?.isChecked = oldCustomField.protectedValue.isProtected

                customFieldDeleteButton?.visibility = View.VISIBLE
                customFieldDeleteButton?.setOnClickListener {
                    entryCustomFieldListener?.onDeleteCustomFieldApproved(oldCustomField)
                    (dialog as AlertDialog?)?.dismiss()
                }
            } ?: run {
                customFieldDeleteButton?.visibility = View.GONE
            }

            val builder = AlertDialog.Builder(activity)
            builder.setView(root)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
            val dialogCreated = builder.create()

            customFieldLabel?.requestFocus()
            customFieldLabel?.imeOptions = EditorInfo.IME_ACTION_DONE
            customFieldLabel?.setOnEditorActionListener { _, actionId, _ ->
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
            oldField?.let {
                // New property with old value
                entryCustomFieldListener?.onEditCustomFieldApproved(it,
                        Field(customFieldLabel?.text?.toString() ?: "",
                                ProtectedString(customFieldProtectionButton?.isChecked == true,
                                        it.protectedValue.stringValue))
                )
            } ?: run {
                entryCustomFieldListener?.onNewCustomFieldApproved(
                        Field(customFieldLabel?.text?.toString() ?: "",
                                ProtectedString(customFieldProtectionButton?.isChecked == true))
                )
            }
            (dialog as AlertDialog?)?.dismiss()
        }
    }

    private fun isValid(): Boolean {
        return if (customFieldLabel?.text?.toString()?.isNotEmpty() != true) {
            setError(R.string.error_string_key)
            false
        } else {
            setError(null)
            true
        }
    }

    fun setError(@StringRes errorId: Int?) {
        customFieldLabelContainer?.error = if (errorId == null) null else {
            requireContext().getString(errorId)
        }
    }

    interface EntryCustomFieldListener {
        fun onNewCustomFieldApproved(newField: Field)
        fun onEditCustomFieldApproved(oldField: Field, newField: Field)
        fun onDeleteCustomFieldApproved(oldField: Field)
    }

    companion object {

        private const val KEY_FIELD = "KEY_FIELD"

        fun getInstance(): EntryCustomFieldDialogFragment {
            return EntryCustomFieldDialogFragment()
        }

        fun getInstance(field: Field): EntryCustomFieldDialogFragment {
            return EntryCustomFieldDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_FIELD, field)
                }
            }
        }
    }
}
