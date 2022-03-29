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
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
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
    private var passwordView: PasswordView? = null

    private var mPasswordField: Field? = null

    private var sliderLength: Slider? = null
    private var lengthEditView: EditText? = null

    private var uppercaseCompound: CompoundButton? = null
    private var lowercaseCompound: CompoundButton? = null
    private var digitsCompound: CompoundButton? = null
    private var minusCompound: CompoundButton? = null
    private var underlineCompound: CompoundButton? = null
    private var spaceCompound: CompoundButton? = null
    private var specialsCompound: CompoundButton? = null
    private var bracketsCompound: CompoundButton? = null
    private var extendedCompound: CompoundButton? = null
    private var considerCharsEditText: EditText? = null
    private var ignoreCharsEditText: EditText? = null
    private var atLeastOneCompound: CompoundButton? = null
    private var excludeAmbiguousCompound: CompoundButton? = null

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

            sliderLength = root?.findViewById(R.id.slider_length)
            lengthEditView = root?.findViewById(R.id.length)

            uppercaseCompound = root?.findViewById(R.id.upperCase_filter)
            lowercaseCompound = root?.findViewById(R.id.lowerCase_filter)
            digitsCompound = root?.findViewById(R.id.digits_filter)
            minusCompound = root?.findViewById(R.id.minus_filter)
            underlineCompound = root?.findViewById(R.id.underline_filter)
            spaceCompound = root?.findViewById(R.id.space_filter)
            specialsCompound = root?.findViewById(R.id.special_filter)
            bracketsCompound = root?.findViewById(R.id.brackets_filter)
            extendedCompound = root?.findViewById(R.id.extendedASCII_filter)
            considerCharsEditText = root?.findViewById(R.id.consider_chars_filter)
            ignoreCharsEditText = root?.findViewById(R.id.ignore_chars_filter)
            atLeastOneCompound = root?.findViewById(R.id.atLeastOne_filter)
            excludeAmbiguousCompound = root?.findViewById(R.id.excludeAmbiguous_filter)

            mPasswordField = arguments?.getParcelable(KEY_PASSWORD_FIELD)

            assignDefaultCharacters()

            uppercaseCompound?.setOnCheckedChangeListener { _, _ ->
                generatePassword()
            }
            lowercaseCompound?.setOnCheckedChangeListener { _, _ ->
                generatePassword()
            }
            digitsCompound?.setOnCheckedChangeListener { _, _ ->
                generatePassword()
            }
            minusCompound?.setOnCheckedChangeListener { _, _ ->
                generatePassword()
            }
            underlineCompound?.setOnCheckedChangeListener { _, _ ->
                generatePassword()
            }
            spaceCompound?.setOnCheckedChangeListener { _, _ ->
                generatePassword()
            }
            specialsCompound?.setOnCheckedChangeListener { _, _ ->
                generatePassword()
            }
            bracketsCompound?.setOnCheckedChangeListener { _, _ ->
                generatePassword()
            }
            extendedCompound?.setOnCheckedChangeListener { _, _ ->
                generatePassword()
            }
            considerCharsEditText?.doOnTextChanged { _, _, _, _ ->
                generatePassword()
            }
            ignoreCharsEditText?.doOnTextChanged { _, _, _, _ ->
                generatePassword()
            }
            atLeastOneCompound?.setOnCheckedChangeListener { _, _ ->
                generatePassword()
            }
            excludeAmbiguousCompound?.setOnCheckedChangeListener { _, _ ->
                generatePassword()
            }

            var listenSlider = true
            var listenEditText = true
            sliderLength?.addOnChangeListener { _, value, _ ->
                try {
                    listenEditText = false
                    if (listenSlider) {
                        lengthEditView?.setText(value.toInt().toString())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to set the length value", e)
                } finally {
                    listenEditText = true
                }
            }
            sliderLength?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                // TODO upgrade material-components lib
                // https://stackoverflow.com/questions/70873160/material-slider-onslidertouchlisteners-methods-can-only-be-called-from-within-t
                @SuppressLint("RestrictedApi")
                override fun onStartTrackingTouch(slider: Slider) {}

                @SuppressLint("RestrictedApi")
                override fun onStopTrackingTouch(slider: Slider) {
                    generatePassword()
                }
            })
            lengthEditView?.doOnTextChanged { _, _, _, _ ->
                if (listenEditText) {
                    try {
                        listenSlider = false
                        setSliderValue(getPasswordLength())
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to get the length value", e)
                    } finally {
                        listenSlider = true
                        generatePassword()
                    }
                }
            }

            root?.findViewById<View>(R.id.generate_password_button)
                    ?.setOnClickListener { generatePassword() }

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
            generatePassword()

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onDestroy() {
        saveOptions()
        super.onDestroy()
    }

    private fun saveOptions() {
        context?.let {
            val optionsSet = mutableSetOf<String>()
            if (uppercaseCompound?.isChecked == true)
                optionsSet.add(getString(R.string.value_password_uppercase))
            if (lowercaseCompound?.isChecked == true)
                optionsSet.add(getString(R.string.value_password_lowercase))
            if (digitsCompound?.isChecked == true)
                optionsSet.add(getString(R.string.value_password_digits))
            if (minusCompound?.isChecked == true)
                optionsSet.add(getString(R.string.value_password_minus))
            if (underlineCompound?.isChecked == true)
                optionsSet.add(getString(R.string.value_password_underline))
            if (spaceCompound?.isChecked == true)
                optionsSet.add(getString(R.string.value_password_space))
            if (specialsCompound?.isChecked == true)
                optionsSet.add(getString(R.string.value_password_special))
            if (bracketsCompound?.isChecked == true)
                optionsSet.add(getString(R.string.value_password_brackets))
            if (extendedCompound?.isChecked == true)
                optionsSet.add(getString(R.string.value_password_extended))
            if (atLeastOneCompound?.isChecked == true)
                optionsSet.add(getString(R.string.value_password_atLeastOne))
            if (excludeAmbiguousCompound?.isChecked == true)
                optionsSet.add(getString(R.string.value_password_excludeAmbiguous))
            PreferencesUtil.setDefaultPasswordCharacters(it, optionsSet)
            PreferencesUtil.setDefaultPasswordLength(it, getPasswordLength())
        }
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
        atLeastOneCompound?.isChecked = false
        excludeAmbiguousCompound?.isChecked = false

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
                        getString(R.string.value_password_atLeastOne) -> atLeastOneCompound?.isChecked = true
                        getString(R.string.value_password_excludeAmbiguous) -> excludeAmbiguousCompound?.isChecked = true
                    }
                }
            }
            val defaultPasswordLength = PreferencesUtil.getDefaultPasswordLength(context)
            setSliderValue(defaultPasswordLength)
            lengthEditView?.setText(defaultPasswordLength.toString())
        }
    }

    private fun getPasswordLength(): Int {
        return try {
            Integer.valueOf(lengthEditView?.text.toString())
        } catch (numberException: NumberFormatException) {
            MIN_SLIDER_LENGTH.toInt()
        }
    }

    private fun setSliderValue(value: Int) {
        val sliderValue = value.toFloat()
        when {
            sliderValue < MIN_SLIDER_LENGTH -> {
                sliderLength?.value = MIN_SLIDER_LENGTH
            }
            sliderValue > MAX_SLIDER_LENGTH -> {
                sliderLength?.value = MAX_SLIDER_LENGTH
            }
            else -> {
                sliderLength?.value = sliderValue
            }
        }
    }

    private fun generatePassword() {
        var password = ""
        try {
            password = PasswordGenerator(resources).generatePassword(getPasswordLength(),
                    uppercaseCompound?.isChecked == true,
                    lowercaseCompound?.isChecked == true,
                    digitsCompound?.isChecked == true,
                    minusCompound?.isChecked == true,
                    underlineCompound?.isChecked == true,
                    spaceCompound?.isChecked == true,
                    specialsCompound?.isChecked == true,
                    bracketsCompound?.isChecked == true,
                    extendedCompound?.isChecked == true,
                    considerCharsEditText?.text?.toString() ?: "",
                    ignoreCharsEditText?.text?.toString() ?: "",
                    atLeastOneCompound?.isChecked == true,
                    excludeAmbiguousCompound?.isChecked == true)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to generate a password", e)
        }
        passwordView?.passwordString = password
    }

    interface GeneratePasswordListener {
        fun acceptPassword(passwordField: Field)
        fun cancelPassword(passwordField: Field)
    }

    companion object {
        private const val KEY_PASSWORD_FIELD = "KEY_PASSWORD_FIELD"
        private const val MIN_SLIDER_LENGTH = 1F
        private const val MAX_SLIDER_LENGTH = 128F
        private const val TAG = "GeneratePasswordDialog";

        fun getInstance(field: Field): GeneratePasswordDialogFragment {
            return GeneratePasswordDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_PASSWORD_FIELD, field)
                }
            }
        }
    }
}
