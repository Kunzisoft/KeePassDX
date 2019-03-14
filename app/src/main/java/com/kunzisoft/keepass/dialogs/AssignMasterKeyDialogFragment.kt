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
package com.kunzisoft.keepass.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.fileselect.KeyFileHelper
import com.kunzisoft.keepass.utils.EmptyUtils
import com.kunzisoft.keepass.utils.UriUtil

class AssignMasterKeyDialogFragment : DialogFragment() {

    private var masterPassword: String? = null
    private var mKeyfile: Uri? = null

    private var rootView: View? = null
    private var passwordCheckBox: CompoundButton? = null
    private var passView: TextView? = null
    private var passConfView: TextView? = null
    private var keyfileCheckBox: CompoundButton? = null
    private var keyfileView: TextView? = null

    private var mListener: AssignPasswordDialogListener? = null

    private var keyFileHelper: KeyFileHelper? = null

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
            throw ClassCastException(activity!!.toString()
                    + " must implement " + AssignPasswordDialogListener::class.java.name)
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        activity?.let { notNullActivity ->
            val builder = AlertDialog.Builder(notNullActivity)
            val inflater = notNullActivity.layoutInflater

            rootView = inflater.inflate(R.layout.set_password, null)
            builder.setView(rootView)
                    .setTitle(R.string.assign_master_key)
                    // Add action buttons
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .setNegativeButton(R.string.cancel) { _, _ -> }

            passwordCheckBox = rootView?.findViewById(R.id.password_checkbox)
            passView = rootView?.findViewById(R.id.pass_password)
            passView?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

                override fun afterTextChanged(editable: Editable) {
                    passwordCheckBox?.isChecked = true
                }
            })
            passConfView = rootView?.findViewById(R.id.pass_conf_password)

            keyfileCheckBox = rootView?.findViewById(R.id.keyfile_checkox)
            keyfileView = rootView?.findViewById(R.id.pass_keyfile)
            keyfileView?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

                override fun afterTextChanged(editable: Editable) {
                    keyfileCheckBox?.isChecked = true
                }
            })

            keyFileHelper = KeyFileHelper(this)
            rootView?.findViewById<View>(R.id.browse_button)?.setOnClickListener { view ->
                keyFileHelper?.openFileOnClickViewListener?.onClick(view) }

            val dialog = builder.create()

            if (passwordCheckBox != null && keyfileCheckBox!= null) {
                dialog.setOnShowListener { dialog1 ->
                    val positiveButton = (dialog1 as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
                    positiveButton.setOnClickListener {

                        masterPassword = ""
                        mKeyfile = null

                        var error = verifyPassword() || verifyFile()

                        if (!passwordCheckBox!!.isChecked && !keyfileCheckBox!!.isChecked) {
                            error = true
                            showNoKeyConfirmationDialog()
                        }

                        if (!error) {
                            mListener!!.onAssignKeyDialogPositiveClick(
                                    passwordCheckBox!!.isChecked, masterPassword,
                                    keyfileCheckBox!!.isChecked, mKeyfile)
                            dismiss()
                        }
                    }
                    val negativeButton = dialog1.getButton(DialogInterface.BUTTON_NEGATIVE)
                    negativeButton.setOnClickListener {
                        mListener!!.onAssignKeyDialogNegativeClick(
                                passwordCheckBox!!.isChecked, masterPassword,
                                keyfileCheckBox!!.isChecked, mKeyfile)
                        dismiss()
                    }
                }
            }

            return dialog
        }

        return super.onCreateDialog(savedInstanceState)
    }

    private fun verifyPassword(): Boolean {
        var error = false
        if (passwordCheckBox!!.isChecked) {
            masterPassword = passView!!.text.toString()
            val confpass = passConfView!!.text.toString()

            // Verify that passwords match
            if (masterPassword != confpass) {
                error = true
                // Passwords do not match
                Toast.makeText(context, R.string.error_pass_match, Toast.LENGTH_LONG).show()
            }

            if (masterPassword == null || masterPassword!!.isEmpty()) {
                error = true
                showEmptyPasswordConfirmationDialog()
            }
        }
        return error
    }

    private fun verifyFile(): Boolean {
        var error = false
        if (keyfileCheckBox!!.isChecked) {
            val keyfile = UriUtil.parseDefaultFile(keyfileView!!.text.toString())
            mKeyfile = keyfile

            // Verify that a keyfile is set
            if (EmptyUtils.isNullOrEmpty(keyfile)) {
                error = true
                Toast.makeText(context, R.string.error_nokeyfile, Toast.LENGTH_LONG).show()
            }
        }
        return error
    }

    private fun showEmptyPasswordConfirmationDialog() {
        val builder = AlertDialog.Builder(activity!!)
        builder.setMessage(R.string.warning_empty_password)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (!verifyFile()) {
                        mListener!!.onAssignKeyDialogPositiveClick(
                                passwordCheckBox!!.isChecked, masterPassword,
                                keyfileCheckBox!!.isChecked, mKeyfile)
                        this@AssignMasterKeyDialogFragment.dismiss()
                    }
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
        builder.create().show()
    }

    private fun showNoKeyConfirmationDialog() {
        val builder = AlertDialog.Builder(activity!!)
        builder.setMessage(R.string.warning_no_encryption_key)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    mListener!!.onAssignKeyDialogPositiveClick(
                            passwordCheckBox!!.isChecked, masterPassword,
                            keyfileCheckBox!!.isChecked, mKeyfile)
                    this@AssignMasterKeyDialogFragment.dismiss()
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
        builder.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        keyFileHelper!!.onActivityResultCallback(requestCode, resultCode, data
        ) { uri ->
            if (uri != null) {
                val pathString = UriUtil.parseDefaultFile(uri.toString())
                if (pathString != null) {
                    keyfileCheckBox!!.isChecked = true
                    keyfileView!!.text = pathString.toString()
                }
            }
        }
    }
}
