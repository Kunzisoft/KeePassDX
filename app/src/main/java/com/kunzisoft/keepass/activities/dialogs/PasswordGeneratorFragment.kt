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
import com.kunzisoft.keepass.activities.fragments.DatabaseFragment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.view.PasswordView
import com.kunzisoft.keepass.viewmodels.KeyGeneratorViewModel

class PasswordGeneratorFragment : DatabaseFragment() {

    private var passwordView: PasswordView? = null

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

    private val mKeyGeneratorViewModel: KeyGeneratorViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_generate_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        passwordView = view.findViewById(R.id.password_view)
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

        contextThemed?.let { context ->
            passwordCopyView?.visibility = if(PreferencesUtil.allowCopyProtectedFields(context))
                View.VISIBLE else View.GONE
            val clipboardHelper = ClipboardHelper(context)
            passwordCopyView?.setOnClickListener {
                clipboardHelper.timeoutCopyToClipboard(passwordView!!.passwordString,
                    getString(R.string.copy_field,
                        getString(R.string.entry_password)))
            }
        }

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

        // Pre-populate a password to possibly save the user a few clicks
        generatePassword()

        mKeyGeneratorViewModel.keyGeneratedValidated.observe(viewLifecycleOwner) {
            mKeyGeneratorViewModel.setKeyGenerated(passwordView?.passwordString ?: "")
        }

        mKeyGeneratorViewModel.requireKeyGeneration.observe(viewLifecycleOwner) {
            generatePassword()
        }

        resetAppTimeoutWhenViewFocusedOrChanged(view)
    }

    override fun onDestroy() {
        saveOptions()
        super.onDestroy()
    }

    override fun onDatabaseRetrieved(database: Database?) {
        // Nothing here
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

    companion object {
        private const val MIN_SLIDER_LENGTH = 1F
        private const val MAX_SLIDER_LENGTH = 128F
        private const val TAG = "PasswordGeneratorFrgmt"
    }
}
