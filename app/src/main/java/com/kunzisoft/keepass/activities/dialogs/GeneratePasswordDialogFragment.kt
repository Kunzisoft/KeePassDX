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

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.slider.Slider
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.view.PasswordView

class GeneratePasswordDialogFragment : DatabaseDialogFragment() {

    private var mListener: GeneratePasswordListener? = null

    private var root: View? = null
    private var lengthTextView: EditText? = null
    private var passwordView: PasswordView? = null

    private var mPasswordField: Field? = null

    private var uppercaseCompound: CompoundButton? = null
    private var lowercaseCompound: CompoundButton? = null
    private var digitsCompound: CompoundButton? = null
    private var minusCompound: CompoundButton? = null
    private var underlineCompound: CompoundButton? = null
    private var spaceCompound: CompoundButton? = null
    private var specialsCompound: CompoundButton? = null
    private var bracketsCompound: CompoundButton? = null
    private var extendedCompound: CompoundButton? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as GeneratePasswordListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString()
                    + " must implement " + GeneratePasswordListener::class.java.name)
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            val inflater = activity.layoutInflater
            root = inflater.inflate(R.layout.fragment_generate_password, null)

            passwordView = root?.findViewById(R.id.password_view)
            val passwordCopyView: ImageView? = root?.findViewById(R.id.password_copy_button)
            passwordCopyView?.visibility = if(PreferencesUtil.allowCopyProtectedFields(activity))
                View.VISIBLE else View.GONE
            val clipboardHelper = ClipboardHelper(activity)
            passwordCopyView?.setOnClickListener {
                clipboardHelper.timeoutCopyToClipboard(passwordView!!.passwordString,
                        getString(R.string.copy_field,
                                getString(R.string.entry_password)))
            }
            lengthTextView = root?.findViewById(R.id.length)

            uppercaseCompound = root?.findViewById(R.id.upperCase_filter)
            lowercaseCompound = root?.findViewById(R.id.lowerCase_filter)
            digitsCompound = root?.findViewById(R.id.digits_filter)
            minusCompound = root?.findViewById(R.id.minus_filter)
            underlineCompound = root?.findViewById(R.id.underline_filter)
            spaceCompound = root?.findViewById(R.id.space_filter)
            specialsCompound = root?.findViewById(R.id.special_filter)
            bracketsCompound = root?.findViewById(R.id.brackets_filter)
            extendedCompound = root?.findViewById(R.id.extendedASCII_filter)

            mPasswordField = arguments?.getParcelable(KEY_PASSWORD_FIELD)

            assignDefaultCharacters()

            val sliderLength = root?.findViewById<Slider>(R.id.slider_length)
            sliderLength?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                @SuppressLint("RestrictedApi")
                override fun onStartTrackingTouch(slider: Slider) {
                }
                @SuppressLint("RestrictedApi")
                override fun onStopTrackingTouch(slider: Slider) {
                    lengthTextView?.setText(slider.value.toInt().toString())
                }
            })

            context?.let { context ->
                sliderLength?.value = PreferencesUtil.getDefaultPasswordLength(context).toFloat()
            }

            root?.findViewById<View>(R.id.generate_password_button)
                    ?.setOnClickListener { fillPassword() }

            builder.setView(root)
                    .setPositiveButton(R.string.accept) { _, _ ->
                        mPasswordField?.let { passwordField ->
                            passwordView?.passwordString?.let { passwordValue ->
                                passwordField.protectedValue.stringValue = passwordValue
                            }
                            mListener?.acceptPassword(passwordField)
                        }
                        dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        mPasswordField?.let { passwordField ->
                            mListener?.cancelPassword(passwordField)
                        }
                        dismiss()
                    }

            // Pre-populate a password to possibly save the user a few clicks
            fillPassword()

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun assignDefaultCharacters() {
        uppercaseCompound?.isChecked = false
        lowercaseCompound?.isChecked = false
        digitsCompound?.isChecked = false
        minusCompound?.isChecked = false
        underlineCompound?.isChecked = false
        spaceCompound?.isChecked = false
        specialsCompound?.isChecked = false
        bracketsCompound?.isChecked = false
        extendedCompound?.isChecked = false

        context?.let { context ->
            PreferencesUtil.getDefaultPasswordCharacters(context)?.let { charSet ->
                for (passwordChar in charSet) {
                    when (passwordChar) {
                        getString(R.string.value_password_uppercase) -> uppercaseCompound?.isChecked = true
                        getString(R.string.value_password_lowercase) -> lowercaseCompound?.isChecked = true
                        getString(R.string.value_password_digits) -> digitsCompound?.isChecked = true
                        getString(R.string.value_password_minus) -> minusCompound?.isChecked = true
                        getString(R.string.value_password_underline) -> underlineCompound?.isChecked = true
                        getString(R.string.value_password_space) -> spaceCompound?.isChecked = true
                        getString(R.string.value_password_special) -> specialsCompound?.isChecked = true
                        getString(R.string.value_password_brackets) -> bracketsCompound?.isChecked = true
                        getString(R.string.value_password_extended) -> extendedCompound?.isChecked = true
                    }
                }
            }
        }
    }

    private fun fillPassword() {
        val passwordGenerated = generatePassword()
        passwordView?.passwordString = passwordGenerated
    }

    private fun generatePassword(): String {
        var password = ""
        try {
            val length = Integer.valueOf(root?.findViewById<EditText>(R.id.length)?.text.toString())
            password = PasswordGenerator(resources).generatePassword(length,
                    uppercaseCompound?.isChecked == true,
                    lowercaseCompound?.isChecked == true,
                    digitsCompound?.isChecked == true,
                    minusCompound?.isChecked == true,
                    underlineCompound?.isChecked == true,
                    spaceCompound?.isChecked == true,
                    specialsCompound?.isChecked == true,
                    bracketsCompound?.isChecked == true,
                    extendedCompound?.isChecked == true)
            passwordView?.error = null
        } catch (e: NumberFormatException) {
            passwordView?.error = getString(R.string.error_wrong_length)
        } catch (e: IllegalArgumentException) {
            passwordView?.error = e.message
        }

        return password
    }

    interface GeneratePasswordListener {
        fun acceptPassword(passwordField: Field)
        fun cancelPassword(passwordField: Field)
    }

    companion object {
        private const val KEY_PASSWORD_FIELD = "KEY_PASSWORD_FIELD"

        fun getInstance(field: Field): GeneratePasswordDialogFragment {
            return GeneratePasswordDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_PASSWORD_FIELD, field)
                }
            }
        }
    }
}
