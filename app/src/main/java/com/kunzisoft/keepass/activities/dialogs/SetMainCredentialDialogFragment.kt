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
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.helpers.setOpenDocumentClickListener
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.hardware.HardwareKeyActivity
import com.kunzisoft.keepass.password.PasswordEntropy
import com.kunzisoft.keepass.utils.UriUtil.getDocumentFile
import com.kunzisoft.keepass.utils.UriUtil.openUrl
import com.kunzisoft.keepass.view.HardwareKeySelectionView
import com.kunzisoft.keepass.view.KeyFileSelectionView
import com.kunzisoft.keepass.view.PassKeyView
import com.kunzisoft.keepass.view.applyFontVisibility

class SetMainCredentialDialogFragment : DatabaseDialogFragment() {

    private var mMasterPassword: String? = null
    private var mKeyFileUri: Uri? = null
    private var mHardwareKey: HardwareKey? = null

    private lateinit var rootView: View

    private lateinit var passwordCheckBox: CompoundButton
    private lateinit var passwordView: PassKeyView
    private lateinit var passwordRepeatTextInputLayout: TextInputLayout
    private lateinit var passwordRepeatView: TextView

    private lateinit var keyFileCheckBox: CompoundButton
    private lateinit var keyFileSelectionView: KeyFileSelectionView

    private lateinit var hardwareKeyCheckBox: CompoundButton
    private lateinit var hardwareKeySelectionView: HardwareKeySelectionView

    private var mListener: AssignMainCredentialDialogListener? = null

    private var mExternalFileHelper: ExternalFileHelper? = null
    private var mPasswordEntropyCalculator: PasswordEntropy? = null

    private var mEmptyPasswordConfirmationDialog: AlertDialog? = null
    private var mNoKeyConfirmationDialog: AlertDialog? = null
    private var mEmptyKeyFileConfirmationDialog: AlertDialog? = null

    private var mAllowNoMasterKey: Boolean  = false

    private val passwordTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun afterTextChanged(editable: Editable) {
            passwordCheckBox.isChecked = true
        }
    }

    interface AssignMainCredentialDialogListener {
        fun onAssignKeyDialogPositiveClick(mainCredential: MainCredential)
        fun onAssignKeyDialogNegativeClick(mainCredential: MainCredential)
    }

    override fun onAttach(activity: Context) {
        super.onAttach(activity)
        try {
            mListener = activity as AssignMainCredentialDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString()
                    + " must implement " + AssignMainCredentialDialogListener::class.java.name)
        }
    }

    override fun onDetach() {
        mListener = null
        mEmptyPasswordConfirmationDialog?.dismiss()
        mEmptyPasswordConfirmationDialog = null
        mNoKeyConfirmationDialog?.dismiss()
        mNoKeyConfirmationDialog = null
        mEmptyKeyFileConfirmationDialog?.dismiss()
        mEmptyKeyFileConfirmationDialog = null
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the password entropy object
        mPasswordEntropyCalculator = PasswordEntropy()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->

            arguments?.apply {
                if (containsKey(ALLOW_NO_MASTER_KEY_ARG))
                    mAllowNoMasterKey = getBoolean(ALLOW_NO_MASTER_KEY_ARG, false)
            }

            val builder = AlertDialog.Builder(activity)
            val inflater = activity.layoutInflater

            rootView = inflater.inflate(R.layout.fragment_set_main_credential, null)
            builder.setView(rootView)
                    // Add action buttons
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }

            rootView.findViewById<View>(R.id.credentials_information)?.setOnClickListener {
                activity.openUrl(R.string.credentials_explanation_url)
            }

            passwordCheckBox = rootView.findViewById(R.id.password_checkbox)
            passwordView = rootView.findViewById(R.id.password_view)
            passwordRepeatTextInputLayout = rootView.findViewById(R.id.password_repeat_input_layout)
            passwordRepeatView = rootView.findViewById(R.id.password_confirmation)
            passwordRepeatView.applyFontVisibility()

            keyFileCheckBox = rootView.findViewById(R.id.keyfile_checkbox)
            keyFileSelectionView = rootView.findViewById(R.id.keyfile_selection)

            hardwareKeyCheckBox = rootView.findViewById(R.id.hardware_key_checkbox)
            hardwareKeySelectionView = rootView.findViewById(R.id.hardware_key_selection)

            mExternalFileHelper = ExternalFileHelper(this)
            mExternalFileHelper?.buildOpenDocument { uri ->
                uri?.let { pathUri ->
                    pathUri.getDocumentFile(requireContext())?.length()?.let { lengthFile ->
                        keyFileSelectionView.error = null
                        keyFileCheckBox.isChecked = true
                        keyFileSelectionView.uri = pathUri
                        if (lengthFile <= 0L) {
                            showEmptyKeyFileConfirmationDialog()
                        }
                    }
                }
            }
            keyFileSelectionView.setOpenDocumentClickListener(mExternalFileHelper)

            hardwareKeySelectionView.selectionListener = { hardwareKey ->
                hardwareKeyCheckBox.isChecked = true
                hardwareKeySelectionView.error =
                    if (!HardwareKeyActivity.isHardwareKeyAvailable(requireActivity(), hardwareKey)) {
                        // show hardware driver dialog if required
                        getString(R.string.error_driver_required, hardwareKey.toString())
                    } else {
                        null
                    }
            }

            val dialog = builder.create()
            dialog.setOnShowListener { dialog1 ->
                val positiveButton = (dialog1 as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {

                    mMasterPassword = ""
                    mKeyFileUri = null
                    mHardwareKey = null

                    approveMainCredential()
                }
                val negativeButton = dialog1.getButton(DialogInterface.BUTTON_NEGATIVE)
                negativeButton.setOnClickListener {
                    mListener?.onAssignKeyDialogNegativeClick(retrieveMainCredential())
                    dismiss()
                }
            }

            return dialog
        }

        return super.onCreateDialog(savedInstanceState)
    }

    private fun approveMainCredential() {
        val errorPassword = verifyPassword()
        val errorKeyFile = verifyKeyFile()
        val errorHardwareKey = verifyHardwareKey()
        // Check all to fill error
        var error = errorPassword || errorKeyFile || errorHardwareKey
        val hardwareKey = hardwareKeySelectionView.hardwareKey
        if (!error
            && (!passwordCheckBox.isChecked)
            && (!keyFileCheckBox.isChecked)
            && (!hardwareKeyCheckBox.isChecked)
        ) {
            error = true
            if (mAllowNoMasterKey) {
                // show no key dialog if required
                showNoKeyConfirmationDialog()
            } else {
                passwordRepeatTextInputLayout.error =
                    getString(R.string.error_disallow_no_credentials)
            }
        } else if (!error
            && mMasterPassword.isNullOrEmpty()
            && !keyFileCheckBox.isChecked
            && !hardwareKeyCheckBox.isChecked
        ) {
            // show empty password dialog if required
            error = true
            showEmptyPasswordConfirmationDialog()
        } else if (!error
            && hardwareKey != null
            && !HardwareKeyActivity.isHardwareKeyAvailable(
                requireActivity(), hardwareKey, false)
        ) {
            // show hardware driver dialog if required
            error = true
            hardwareKeySelectionView.error =
                getString(R.string.error_driver_required, hardwareKey.toString())
        }
        if (!error) {
            mListener?.onAssignKeyDialogPositiveClick(retrieveMainCredential())
            dismiss()
        }
    }

    private fun verifyPassword(): Boolean {
        var error = false
        passwordRepeatTextInputLayout.error = null
        if (passwordCheckBox.isChecked) {
            mMasterPassword = passwordView.passwordString
            val confPassword = passwordRepeatView.text.toString()

            // Verify that passwords match
            if (mMasterPassword != confPassword) {
                error = true
                // Passwords do not match
                passwordRepeatTextInputLayout.error = getString(R.string.error_pass_match)
            }
        }
        return error
    }

    private fun verifyKeyFile(): Boolean {
        var error = false
        keyFileSelectionView.error = null
        if (keyFileCheckBox.isChecked) {
            keyFileSelectionView.uri?.let { uri ->
                mKeyFileUri = uri
            } ?: run {
                error = true
                keyFileSelectionView.error = getString(R.string.error_nokeyfile)
            }
        }
        return error
    }

    private fun verifyHardwareKey(): Boolean {
        var error = false
        hardwareKeySelectionView.error = null
        if (hardwareKeyCheckBox.isChecked) {
            hardwareKeySelectionView.hardwareKey?.let { hardwareKey ->
                mHardwareKey = hardwareKey
            } ?: run {
                error = true
                hardwareKeySelectionView.error = getString(R.string.error_no_hardware_key)
            }
        }
        return error
    }

    private fun retrieveMainCredential(): MainCredential {
        val masterPassword = if (passwordCheckBox.isChecked) mMasterPassword else null
        val keyFileUri = if (keyFileCheckBox.isChecked) mKeyFileUri else null
        val hardwareKey = if (hardwareKeyCheckBox.isChecked) mHardwareKey else null
        return MainCredential(masterPassword, keyFileUri, hardwareKey)
    }

    override fun onResume() {
        super.onResume()

        // To check checkboxes if a text is present
        passwordView.addTextChangedListener(passwordTextWatcher)
    }

    override fun onPause() {
        super.onPause()

        passwordView.removeTextChangedListener(passwordTextWatcher)
    }

    private fun showEmptyPasswordConfirmationDialog() {
        activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.warning_empty_password)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        mListener?.onAssignKeyDialogPositiveClick(retrieveMainCredential())
                        this@SetMainCredentialDialogFragment.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
            mEmptyPasswordConfirmationDialog = builder.create()
            mEmptyPasswordConfirmationDialog?.show()
        }
    }

    private fun showNoKeyConfirmationDialog() {
        activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.warning_no_encryption_key)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        mListener?.onAssignKeyDialogPositiveClick(retrieveMainCredential())
                        this@SetMainCredentialDialogFragment.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
            mNoKeyConfirmationDialog = builder.create()
            mNoKeyConfirmationDialog?.show()
        }
    }

    private fun showEmptyKeyFileConfirmationDialog() {
        activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(SpannableStringBuilder().apply {
                append(getString(R.string.warning_empty_keyfile))
                append("\n\n")
                append(getString(R.string.warning_empty_keyfile_explanation))
                append("\n\n")
                append(getString(R.string.warning_sure_add_file))
                })
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        keyFileCheckBox.isChecked = false
                        keyFileSelectionView.uri = null
                    }
            mEmptyKeyFileConfirmationDialog = builder.create()
            mEmptyKeyFileConfirmationDialog?.show()
        }
    }

    companion object {

        private const val ALLOW_NO_MASTER_KEY_ARG = "ALLOW_NO_MASTER_KEY_ARG"

        fun getInstance(allowNoMasterKey: Boolean): SetMainCredentialDialogFragment {
            val fragment = SetMainCredentialDialogFragment()
            val args = Bundle()
            args.putBoolean(ALLOW_NO_MASTER_KEY_ARG, allowNoMasterKey)
            fragment.arguments = args
            return fragment
        }
    }
}
