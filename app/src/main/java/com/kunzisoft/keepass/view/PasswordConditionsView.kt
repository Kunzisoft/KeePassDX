/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.widget.doOnTextChanged
import com.google.android.material.slider.Slider
import com.kunzisoft.keepass.R

class PasswordConditionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val sliderLength: Slider
    private val lengthEditView: EditText

    private val uppercaseFilterView: CompoundButton
    private val lowercaseFilterView: CompoundButton
    private val digitsFilterView: CompoundButton
    private val minusFilterView: CompoundButton
    private val underlineFilterView: CompoundButton
    private val spaceFilterView: CompoundButton
    private val specialsFilterView: CompoundButton
    private val bracketsFilterView: CompoundButton
    private val extendedFilterView: CompoundButton
    private val considerCharsEditText: EditText
    private val ignoreCharsEditText: EditText
    private val atLeastOneFilterView: CompoundButton
    private val excludeAmbiguousFilterView: CompoundButton

    private val minLengthSlider: Int
    private val maxLengthSlider: Int

    var onConditionsChanged: (() -> Unit)? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_password_conditions, this)

        sliderLength = findViewById(R.id.slider_length)
        lengthEditView = findViewById(R.id.length)

        uppercaseFilterView = findViewById(R.id.upperCase_filter)
        lowercaseFilterView = findViewById(R.id.lowerCase_filter)
        digitsFilterView = findViewById(R.id.digits_filter)
        minusFilterView = findViewById(R.id.minus_filter)
        underlineFilterView = findViewById(R.id.underline_filter)
        spaceFilterView = findViewById(R.id.space_filter)
        specialsFilterView = findViewById(R.id.special_filter)
        bracketsFilterView = findViewById(R.id.brackets_filter)
        extendedFilterView = findViewById(R.id.extendedASCII_filter)
        considerCharsEditText = findViewById(R.id.consider_chars_filter)
        ignoreCharsEditText = findViewById(R.id.ignore_chars_filter)
        atLeastOneFilterView = findViewById(R.id.atLeastOne_filter)
        excludeAmbiguousFilterView = findViewById(R.id.excludeAmbiguous_filter)

        minLengthSlider = resources.getInteger(R.integer.password_generator_length_min)
        maxLengthSlider = resources.getInteger(R.integer.password_generator_length_max)

        uppercaseFilterView.setOnCheckedChangeListener { _, _ ->
            onConditionsChanged?.invoke()
        }
        lowercaseFilterView.setOnCheckedChangeListener { _, _ ->
            onConditionsChanged?.invoke()
        }
        digitsFilterView.setOnCheckedChangeListener { _, _ ->
            onConditionsChanged?.invoke()
        }
        minusFilterView.setOnCheckedChangeListener { _, _ ->
            onConditionsChanged?.invoke()
        }
        underlineFilterView.setOnCheckedChangeListener { _, _ ->
            onConditionsChanged?.invoke()
        }
        spaceFilterView.setOnCheckedChangeListener { _, _ ->
            onConditionsChanged?.invoke()
        }
        specialsFilterView.setOnCheckedChangeListener { _, _ ->
            onConditionsChanged?.invoke()
        }
        bracketsFilterView.setOnCheckedChangeListener { _, _ ->
            onConditionsChanged?.invoke()
        }
        extendedFilterView.setOnCheckedChangeListener { _, _ ->
            onConditionsChanged?.invoke()
        }
        considerCharsEditText.doOnTextChanged { _, _, _, _ ->
            onConditionsChanged?.invoke()
        }
        ignoreCharsEditText.doOnTextChanged { _, _, _, _ ->
            onConditionsChanged?.invoke()
        }
        atLeastOneFilterView.setOnCheckedChangeListener { _, _ ->
            onConditionsChanged?.invoke()
        }
        excludeAmbiguousFilterView.setOnCheckedChangeListener { _, _ ->
            onConditionsChanged?.invoke()
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
                onConditionsChanged?.invoke()
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
                    onConditionsChanged?.invoke()
                }
            }
        }
    }

    fun getPasswordLength(): Int {
        return try {
            Integer.valueOf(lengthEditView.text.toString())
        } catch (_: NumberFormatException) {
            minLengthSlider
        }
    }

    fun setPasswordLength(passwordLength: Int) {
        setSliderValue(passwordLength)
        lengthEditView.setText(passwordLength.toString())
    }

    fun getOptions(): Set<String> {
        val optionsSet = mutableSetOf<String>()
        if (uppercaseFilterView.isChecked)
            optionsSet.add(context.getString(R.string.value_password_uppercase))
        if (lowercaseFilterView.isChecked)
            optionsSet.add(context.getString(R.string.value_password_lowercase))
        if (digitsFilterView.isChecked)
            optionsSet.add(context.getString(R.string.value_password_digits))
        if (minusFilterView.isChecked)
            optionsSet.add(context.getString(R.string.value_password_minus))
        if (underlineFilterView.isChecked)
            optionsSet.add(context.getString(R.string.value_password_underline))
        if (spaceFilterView.isChecked)
            optionsSet.add(context.getString(R.string.value_password_space))
        if (specialsFilterView.isChecked)
            optionsSet.add(context.getString(R.string.value_password_special))
        if (bracketsFilterView.isChecked)
            optionsSet.add(context.getString(R.string.value_password_brackets))
        if (extendedFilterView.isChecked)
            optionsSet.add(context.getString(R.string.value_password_extended))
        if (atLeastOneFilterView.isChecked)
            optionsSet.add(context.getString(R.string.value_password_atLeastOne))
        if (excludeAmbiguousFilterView.isChecked)
            optionsSet.add(context.getString(R.string.value_password_excludeAmbiguous))
        return optionsSet
    }

    fun setOptions(options: Set<String>) {
        uppercaseFilterView.isChecked = false
        lowercaseFilterView.isChecked = false
        digitsFilterView.isChecked = false
        minusFilterView.isChecked = false
        underlineFilterView.isChecked = false
        spaceFilterView.isChecked = false
        specialsFilterView.isChecked = false
        bracketsFilterView.isChecked = false
        extendedFilterView.isChecked = false
        atLeastOneFilterView.isChecked = false
        excludeAmbiguousFilterView.isChecked = false
        for (option in options) {
            when (option) {
                context.getString(R.string.value_password_uppercase) -> uppercaseFilterView.isChecked = true
                context.getString(R.string.value_password_lowercase) -> lowercaseFilterView.isChecked = true
                context.getString(R.string.value_password_digits) -> digitsFilterView.isChecked = true
                context.getString(R.string.value_password_minus) -> minusFilterView.isChecked = true
                context.getString(R.string.value_password_underline) -> underlineFilterView.isChecked = true
                context.getString(R.string.value_password_space) -> spaceFilterView.isChecked = true
                context.getString(R.string.value_password_special) -> specialsFilterView.isChecked = true
                context.getString(R.string.value_password_brackets) -> bracketsFilterView.isChecked = true
                context.getString(R.string.value_password_extended) -> extendedFilterView.isChecked = true
                context.getString(R.string.value_password_atLeastOne) -> atLeastOneFilterView.isChecked = true
                context.getString(R.string.value_password_excludeAmbiguous) -> excludeAmbiguousFilterView.isChecked = true
            }
        }
    }

    fun getConsiderChars(): String {
        return considerCharsEditText.text.toString()
    }

    fun setConsiderChars(chars: String) {
        considerCharsEditText.setText(chars)
    }

    fun getIgnoreChars(): String {
        return ignoreCharsEditText.text.toString()
    }

    fun setIgnoreChars(chars: String) {
        ignoreCharsEditText.setText(chars)
    }

    fun isUppercaseChecked(): Boolean {
        return uppercaseFilterView.isChecked
    }

    fun isLowercaseChecked(): Boolean {
        return lowercaseFilterView.isChecked
    }

    fun isDigitsChecked(): Boolean {
        return digitsFilterView.isChecked
    }

    fun isMinusChecked(): Boolean {
        return minusFilterView.isChecked
    }

    fun isUnderlineChecked(): Boolean {
        return underlineFilterView.isChecked
    }

    fun isSpaceChecked(): Boolean {
        return spaceFilterView.isChecked
    }

    fun isSpecialsChecked(): Boolean {
        return specialsFilterView.isChecked
    }

    fun isBracketsChecked(): Boolean {
        return bracketsFilterView.isChecked
    }

    fun isExtendedChecked(): Boolean {
        return extendedFilterView.isChecked
    }

    fun isAtLeastOneChecked(): Boolean {
        return atLeastOneFilterView.isChecked
    }

    fun isExcludeAmbiguousChecked(): Boolean {
        return excludeAmbiguousFilterView.isChecked
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
        private val TAG = PasswordConditionsView::class.simpleName
    }
}
