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
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doOnTextChanged
import com.google.android.material.slider.Slider
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.password.PassphraseGenerator

class PassphraseConditionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    private val wordCountSlider: Slider
    private val wordCountEditText: EditText
    private val wordCaseSpinner: Spinner
    private val charactersCountText: TextView

    private val minSliderWordCount: Int
    private val maxSliderWordCount: Int
    private var wordCaseAdapter: ArrayAdapter<String>? = null

    var onConditionsChanged: (() -> Unit)? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_passphrase_conditions, this)

        wordCountSlider = findViewById(R.id.passphrase_slider_word_count)
        wordCountEditText = findViewById(R.id.passphrase_word_count_edit_text)
        wordCaseSpinner = findViewById(R.id.passphrase_word_case_selection)
        charactersCountText = findViewById(R.id.passphrase_character_count)

        minSliderWordCount = resources.getInteger(R.integer.passphrase_generator_word_count_min)
        maxSliderWordCount = resources.getInteger(R.integer.passphrase_generator_word_count_max)

        wordCaseAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.word_case_array)
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        wordCaseSpinner.adapter = wordCaseAdapter

        var listenSlider = true
        var listenEditText = true
        wordCountSlider.addOnChangeListener { _, value, _ ->
            try {
                listenEditText = false
                if (listenSlider) {
                    wordCountEditText.setText(value.toInt().toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to set the word count value", e)
            } finally {
                listenEditText = true
            }
        }
        wordCountSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            // TODO upgrade material-components lib
            // https://stackoverflow.com/questions/70873160/material-slider-onslidertouchlisteners-methods-can-only-be-called-from-within-t
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {}

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                onConditionsChanged?.invoke()
            }
        })
        wordCountEditText.doOnTextChanged { _, _, _, _ ->
            if (listenEditText) {
                try {
                    listenSlider = false
                    setSliderValue(getWordCount())
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to get the word count value", e)
                } finally {
                    listenSlider = true
                    onConditionsChanged?.invoke()
                }
            }
        }
        wordCaseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onConditionsChanged?.invoke()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun getWordCount(): Int {
        return try {
            Integer.valueOf(wordCountEditText.text.toString())
        } catch (_: NumberFormatException) {
            minSliderWordCount
        }
    }

    fun setWordCount(wordCount: Int) {
        setSliderValue(wordCount)
        wordCountEditText.setText(wordCount.toString())
    }

    fun getWordCase(): PassphraseGenerator.WordCase {
        var wordCase = PassphraseGenerator.WordCase.LOWER_CASE
        try {
            wordCase = PassphraseGenerator.WordCase.getByOrdinal(wordCaseSpinner.selectedItemPosition)
        } catch (caseException: Exception) {
            Log.e(TAG, "Unable to retrieve the word case", caseException)
        }
        return wordCase
    }

    fun setWordCase(wordCase: PassphraseGenerator.WordCase) {
        wordCaseSpinner.setSelection(wordCase.ordinal)
    }

    fun setCharacterCountText(text: String) {
        charactersCountText.text = text
    }

    private fun setSliderValue(value: Int) {
        when {
            value < minSliderWordCount -> {
                wordCountSlider.value = minSliderWordCount.toFloat()
            }
            value > maxSliderWordCount -> {
                wordCountSlider.value = maxSliderWordCount.toFloat()
            }
            else -> {
                wordCountSlider.value = value.toFloat()
            }
        }
    }

    companion object {
        private val TAG = PassphraseConditionsView::class.simpleName
    }
}
