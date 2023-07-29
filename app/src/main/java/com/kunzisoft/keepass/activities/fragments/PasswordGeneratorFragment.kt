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
package com.kunzisoft.keepass.activities.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.google.android.material.slider.Slider
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.view.PassKeyView
import com.kunzisoft.keepass.viewmodels.KeyGeneratorViewModel

class PasswordGeneratorFragment : DatabaseFragment() {

    private lateinit var passKeyView: PassKeyView

    private lateinit var sliderLength: Slider
    private lateinit var lengthEditView: EditText

    private lateinit var uppercaseCompound: CompoundButton
    private lateinit var lowercaseCompound: CompoundButton
    private lateinit var digitsCompound: CompoundButton
    private lateinit var minusCompound: CompoundButton
    private lateinit var underlineCompound: CompoundButton
    private lateinit var spaceCompound: CompoundButton
    private lateinit var specialsCompound: CompoundButton
    private lateinit var bracketsCompound: CompoundButton
    private lateinit var extendedCompound: CompoundButton
    private lateinit var considerCharsEditText: EditText
    private lateinit var ignoreCharsEditText: EditText
    private lateinit var atLeastOneCompound: CompoundButton
    private lateinit var excludeAmbiguousCompound: CompoundButton

    private var minLengthSlider: Int = 0
    private var maxLengthSlider: Int = 0

    private val mKeyGeneratorViewModel: KeyGeneratorViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_generate_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        passKeyView = view.findViewById(R.id.password_view)
        val passwordCopyView: ImageView? = view.findViewById(R.id.password_copy_button)

        sliderLength = view.findViewById(R.id.slider_length)
        lengthEditView = view.findViewById(R.id.length)

        uppercaseCompound = view.findViewById(R.id.upperCase_filter)
        lowercaseCompound = view.findViewById(R.id.lowerCase_filter)
        digitsCompound = view.findViewById(R.id.digits_filter)
        minusCompound = view.findViewById(R.id.minus_filter)
        underlineCompound = view.findViewById(R.id.underline_filter)
        spaceCompound = view.findViewById(R.id.space_filter)
        specialsCompound = view.findViewById(R.id.special_filter)
        bracketsCompound = view.findViewById(R.id.brackets_filter)
        extendedCompound = view.findViewById(R.id.extendedASCII_filter)
        considerCharsEditText = view.findViewById(R.id.consider_chars_filter)
        ignoreCharsEditText = view.findViewById(R.id.ignore_chars_filter)
        atLeastOneCompound = view.findViewById(R.id.atLeastOne_filter)
        excludeAmbiguousCompound = view.findViewById(R.id.excludeAmbiguous_filter)

        context?.let { context ->
            passwordCopyView?.visibility = if(PreferencesUtil.allowCopyProtectedFields(context))
                View.VISIBLE else View.GONE
            val clipboardHelper = ClipboardHelper(context)
            passwordCopyView?.setOnClickListener {
                clipboardHelper.timeoutCopyToClipboard(
                    getString(R.string.password),
                    passKeyView.passwordString,
                    true
                )
            }
        }

        minLengthSlider = resources.getInteger(R.integer.password_generator_length_min)
        maxLengthSlider = resources.getInteger(R.integer.password_generator_length_max)

        loadSettings()

        uppercaseCompound.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }
        lowercaseCompound.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }
        digitsCompound.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }
        minusCompound.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }
        underlineCompound.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }
        spaceCompound.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }
        specialsCompound.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }
        bracketsCompound.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }
        extendedCompound.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }
        considerCharsEditText.doOnTextChanged { _, _, _, _ ->
            generatePassword()
        }
        ignoreCharsEditText.doOnTextChanged { _, _, _, _ ->
            generatePassword()
        }
        atLeastOneCompound.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }
        excludeAmbiguousCompound.setOnCheckedChangeListener { _, _ ->
            generatePassword()
        }

        var listenSlider = true
        var listenEditText = true
        sliderLength.addOnChangeListener { _, value, _ ->
            try {
                listenEditText = false
                if (listenSlider) {
                    lengthEditView.setText(value.toInt().toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to set the length value", e)
            } finally {
                listenEditText = true
            }
        }
        sliderLength.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            // TODO upgrade material-components lib
            // https://stackoverflow.com/questions/70873160/material-slider-onslidertouchlisteners-methods-can-only-be-called-from-within-t
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {}

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                generatePassword()
            }
        })
        lengthEditView.doOnTextChanged { _, _, _, _ ->
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

        // Pre-populate a password to possibly save the user a few clicks
        generatePassword()

        mKeyGeneratorViewModel.passwordGeneratedValidated.observe(viewLifecycleOwner) {
            mKeyGeneratorViewModel.setKeyGenerated(passKeyView.passwordString)
        }

        mKeyGeneratorViewModel.requirePasswordGeneration.observe(viewLifecycleOwner) {
            generatePassword()
        }

        resetAppTimeoutWhenViewFocusedOrChanged(view)
    }

    private fun getPasswordLength(): Int {
        return try {
            Integer.valueOf(lengthEditView.text.toString())
        } catch (numberException: NumberFormatException) {
            minLengthSlider
        }
    }

    private fun setPasswordLength(passwordLength: Int) {
        setSliderValue(passwordLength)
        lengthEditView.setText(passwordLength.toString())
    }

    private fun getOptions(): Set<String> {
        val optionsSet = mutableSetOf<String>()
        if (uppercaseCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_uppercase))
        if (lowercaseCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_lowercase))
        if (digitsCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_digits))
        if (minusCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_minus))
        if (underlineCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_underline))
        if (spaceCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_space))
        if (specialsCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_special))
        if (bracketsCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_brackets))
        if (extendedCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_extended))
        if (atLeastOneCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_atLeastOne))
        if (excludeAmbiguousCompound.isChecked)
            optionsSet.add(getString(R.string.value_password_excludeAmbiguous))
        return optionsSet
    }

    private fun setOptions(options: Set<String>) {
        uppercaseCompound.isChecked = false
        lowercaseCompound.isChecked = false
        digitsCompound.isChecked = false
        minusCompound.isChecked = false
        underlineCompound.isChecked = false
        spaceCompound.isChecked = false
        specialsCompound.isChecked = false
        bracketsCompound.isChecked = false
        extendedCompound.isChecked = false
        atLeastOneCompound.isChecked = false
        excludeAmbiguousCompound.isChecked = false
        for (option in options) {
            when (option) {
                getString(R.string.value_password_uppercase) -> uppercaseCompound.isChecked = true
                getString(R.string.value_password_lowercase) -> lowercaseCompound.isChecked = true
                getString(R.string.value_password_digits) -> digitsCompound.isChecked = true
                getString(R.string.value_password_minus) -> minusCompound.isChecked = true
                getString(R.string.value_password_underline) -> underlineCompound.isChecked = true
                getString(R.string.value_password_space) -> spaceCompound.isChecked = true
                getString(R.string.value_password_special) -> specialsCompound.isChecked = true
                getString(R.string.value_password_brackets) -> bracketsCompound.isChecked = true
                getString(R.string.value_password_extended) -> extendedCompound.isChecked = true
                getString(R.string.value_password_atLeastOne) -> atLeastOneCompound.isChecked = true
                getString(R.string.value_password_excludeAmbiguous) -> excludeAmbiguousCompound.isChecked = true
            }
        }
    }

    private fun getConsiderChars(): String {
        return considerCharsEditText.text.toString()
    }

    private fun setConsiderChars(chars: String) {
        considerCharsEditText.setText(chars)
    }

    private fun getIgnoreChars(): String {
        return ignoreCharsEditText.text.toString()
    }

    private fun setIgnoreChars(chars: String) {
        ignoreCharsEditText.setText(chars)
    }

    private fun generatePassword() {
        var password = ""
        try {
            password = PasswordGenerator(resources).generatePassword(getPasswordLength(),
                uppercaseCompound.isChecked,
                lowercaseCompound.isChecked,
                digitsCompound.isChecked,
                minusCompound.isChecked,
                underlineCompound.isChecked,
                spaceCompound.isChecked,
                specialsCompound.isChecked,
                bracketsCompound.isChecked,
                extendedCompound.isChecked,
                getConsiderChars(),
                getIgnoreChars(),
                atLeastOneCompound.isChecked,
                excludeAmbiguousCompound.isChecked)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to generate a password", e)
        }
        passKeyView.passwordString = password
    }

    override fun onDestroy() {
        saveSettings()
        super.onDestroy()
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        // Nothing here
    }

    private fun saveSettings() {
        context?.let { context ->
            PreferencesUtil.setDefaultPasswordOptions(context, getOptions())
            PreferencesUtil.setDefaultPasswordLength(context, getPasswordLength())
            PreferencesUtil.setDefaultPasswordConsiderChars(context, getConsiderChars())
            PreferencesUtil.setDefaultPasswordIgnoreChars(context, getIgnoreChars())
        }
    }

    private fun loadSettings() {
        context?.let { context ->
            setOptions(PreferencesUtil.getDefaultPasswordOptions(context))
            setPasswordLength(PreferencesUtil.getDefaultPasswordLength(context))
            setConsiderChars(PreferencesUtil.getDefaultPasswordConsiderChars(context))
            setIgnoreChars(PreferencesUtil.getDefaultPasswordIgnoreChars(context))
        }
    }

    private fun setSliderValue(value: Int) {
        when {
            value < minLengthSlider -> {
                sliderLength.value = minLengthSlider.toFloat()
            }
            value > maxLengthSlider -> {
                sliderLength.value = maxLengthSlider.toFloat()
            }
            else -> {
                sliderLength.value = value.toFloat()
            }
        }
    }

    companion object {
        private const val TAG = "PasswordGeneratorFrgmt"
    }
}
