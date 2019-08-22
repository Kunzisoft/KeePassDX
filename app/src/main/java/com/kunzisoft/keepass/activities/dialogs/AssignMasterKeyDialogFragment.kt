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
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.KeyFileHelper
import com.kunzisoft.keepass.utils.UriUtil

class AssignMasterKeyDialogFragment : DialogFragment() {

    private var mMasterPassword: String? = null
    private var mKeyFile: Uri? = null

    private var rootView: View? = null

    private var passwordCheckBox: CompoundButton? = null
    private var passwordView: TextView? = null
    private var passwordRepeatTextInputLayout: TextInputLayout? = null
    private var passwordRepeatView: TextView? = null

    private var keyFileTextInputLayout: TextInputLayout? = null
    private var keyFileCheckBox: CompoundButton? = null
    private var keyFileView: TextView? = null

    private var mListener: AssignPasswordDialogListener? = null

    private var mKeyFileHelper: KeyFileHelper? = null

    private val passwordTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun afterTextChanged(editable: Editable) {
            passwordCheckBox?.isChecked = true
        }
    }

    private val keyFileTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun afterTextChanged(editable: Editable) {
            keyFileCheckBox?.isChecked = true
        }
    }

    interface AssignPasswordDialogListener {
        fun onAssignKeyDialogPositiveClick(masterPasswordChecked: Boolean, masterPassword: String?,
                                           keyFileChecked: Boolean, keyFile: Uri?)
        fun onAssignKeyDialogNegativeClick(masterPasswordChecked: Boolean, masterPassword: String?,
                                           keyFileChecked: Boolean, keyFile: Uri?)
    }

    override fun onAttach(activity: Context?) {
        super.onAttach(activity)
        try {
            mListener = activity as AssignPasswordDialogListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity?.toString()
                    + " must implement " + AssignPasswordDialogListener::class.java.name)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            val inflater = activity.layoutInflater

            rootView = inflater.inflate(R.layout.fragment_set_password, null)
            builder.setView(rootView)
                    .setTitle(R.string.assign_master_key)
                    // Add action buttons
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .setNegativeButton(R.string.cancel) { _, _ -> }

            passwordCheckBox = rootView?.findViewById(R.id.password_checkbox)
            passwordView = rootView?.findViewById(R.id.pass_password)
            passwordRepeatTextInputLayout = rootView?.findViewById(R.id.password_repeat_input_layout)
            passwordRepeatView = rootView?.findViewById(R.id.pass_conf_password)

            keyFileTextInputLayout = rootView?.findViewById(R.id.keyfile_input_layout)
            keyFileCheckBox = rootView?.findViewById(R.id.keyfile_checkox)
            keyFileView = rootView?.findViewById(R.id.pass_keyfile)

            mKeyFileHelper = KeyFileHelper(this)
            rootView?.findViewById<View>(R.id.browse_button)?.setOnClickListener { view ->
                mKeyFileHelper?.openFileOnClickViewListener?.onClick(view) }

            val dialog = builder.create()

            if (passwordCheckBox != null && keyFileCheckBox!= null) {
                dialog.setOnShowListener { dialog1 ->
                    val positiveButton = (dialog1 as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
                    positiveButton.setOnClickListener {

                        mMasterPassword = ""
                        mKeyFile = null

                        var error = verifyPassword() || verifyFile()
                        if (!passwordCheckBox!!.isChecked && !keyFileCheckBox!!.isChecked) {
                            error = true
                            showNoKeyConfirmationDialog()
                        }
                        if (!error) {
                            mListener?.onAssignKeyDialogPositiveClick(
                                    passwordCheckBox!!.isChecked, mMasterPassword,
                                    keyFileCheckBox!!.isChecked, mKeyFile)
                            dismiss()
                        }
                    }
                    val negativeButton = dialog1.getButton(DialogInterface.BUTTON_NEGATIVE)
                    negativeButton.setOnClickListener {
                        mListener?.onAssignKeyDialogNegativeClick(
                                passwordCheckBox!!.isChecked, mMasterPassword,
                                keyFileCheckBox!!.isChecked, mKeyFile)
                        dismiss()
                    }
                }
            }

            return dialog
        }

        return super.onCreateDialog(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        // To check checkboxes if a text is present
        passwordView?.addTextChangedListener(passwordTextWatcher)
        keyFileView?.addTextChangedListener(keyFileTextWatcher)
    }

    override fun onPause() {
        super.onPause()

        passwordView?.removeTextChangedListener(passwordTextWatcher)
        keyFileView?.removeTextChangedListener(keyFileTextWatcher)
    }

    private fun verifyPassword(): Boolean {
        var error = false
        if (passwordCheckBox != null
                && passwordCheckBox!!.isChecked
                && passwordView != null
                && passwordRepeatView != null) {
            mMasterPassword = passwordView!!.text.toString()
            val confPassword = passwordRepeatView!!.text.toString()

            // Verify that passwords match
            if (mMasterPassword != confPassword) {
                error = true
                // Passwords do not match
                passwordRepeatTextInputLayout?.error = getString(R.string.error_pass_match)
            }

            if (mMasterPassword == null || mMasterPassword!!.isEmpty()) {
                error = true
                showEmptyPasswordConfirmationDialog()
            }
        }
        return error
    }

    private fun verifyFile(): Boolean {
        var error = false
        if (keyFileCheckBox != null
                && keyFileCheckBox!!.isChecked) {
            val keyFile = UriUtil.parseUriFile(keyFileView?.text?.toString())
            mKeyFile = keyFile

            // Verify that a keyfile is set
            if (keyFile == null || keyFile.toString().isEmpty()) {
                error = true
                // TODO better keyfile check
                keyFileTextInputLayout?.error = getString(R.string.error_nokeyfile)
            }
        }
        return error
    }

    private fun showEmptyPasswordConfirmationDialog() {
        activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.warning_empty_password)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        if (!verifyFile()) {
                            mListener?.onAssignKeyDialogPositiveClick(
                                    passwordCheckBox!!.isChecked, mMasterPassword,
                                    keyFileCheckBox!!.isChecked, mKeyFile)
                            this@AssignMasterKeyDialogFragment.dismiss()
                        }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
            builder.create().show()
        }
    }

    private fun showNoKeyConfirmationDialog() {
        activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.warning_no_encryption_key)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        mListener?.onAssignKeyDialogPositiveClick(
                                passwordCheckBox!!.isChecked, mMasterPassword,
                                keyFileCheckBox!!.isChecked, mKeyFile)
                        this@AssignMasterKeyDialogFragment.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
            builder.create().show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mKeyFileHelper?.onActivityResultCallback(requestCode, resultCode, data
        ) { uri ->
            UriUtil.parseUriFile(uri)?.let { pathUri ->
                keyFileCheckBox?.isChecked = true
                keyFileView?.text = pathUri.toString()

            }
        }
    }
}
