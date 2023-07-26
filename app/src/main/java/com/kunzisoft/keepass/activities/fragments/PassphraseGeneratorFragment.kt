/*
 * Copyright 2022 Jeremy Jamet / Kunzisoft.
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
import android.widget.*
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.google.android.material.slider.Slider
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.password.PassphraseGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.view.PassKeyView
import com.kunzisoft.keepass.viewmodels.KeyGeneratorViewModel

class PassphraseGeneratorFragment : DatabaseFragment() {

    private lateinit var passKeyView: PassKeyView

    private lateinit var sliderWordCount: Slider
    private lateinit var wordCountText: EditText
    private lateinit var charactersCountText: TextView
    private lateinit var wordSeparator: EditText
    private lateinit var wordCaseSpinner: Spinner

    private var minSliderWordCount: Int = 0
    private var maxSliderWordCount: Int = 0
    private var wordCaseAdapter: ArrayAdapter<String>? = null

    private val mKeyGeneratorViewModel: KeyGeneratorViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_generate_passphrase, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        passKeyView = view.findViewById(R.id.passphrase_view)
        val passphraseCopyView: ImageView? = view.findViewById(R.id.passphrase_copy_button)
        sliderWordCount = view.findViewById(R.id.slider_word_count)
        wordCountText = view.findViewById(R.id.word_count)
        charactersCountText = view.findViewById(R.id.character_count)
        wordSeparator = view.findViewById(R.id.word_separator)
        wordCaseSpinner = view.findViewById(R.id.word_case)

        minSliderWordCount = resources.getInteger(R.integer.passphrase_generator_word_count_min)
        maxSliderWordCount = resources.getInteger(R.integer.passphrase_generator_word_count_max)

        context?.let { context ->
            passphraseCopyView?.visibility = if(PreferencesUtil.allowCopyProtectedFields(context))
                View.VISIBLE else View.GONE
            val clipboardHelper = ClipboardHelper(context)
            passphraseCopyView?.setOnClickListener {
                clipboardHelper.timeoutCopyToClipboard(
                    getString(R.string.passphrase),
                    passKeyView.passwordString,
                    true
                )
            }

            wordCaseAdapter = ArrayAdapter(context,
                android.R.layout.simple_spinner_item, resources.getStringArray(R.array.word_case_array)).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            wordCaseSpinner.adapter = wordCaseAdapter
        }

        loadSettings()

        var listenSlider = true
        var listenEditText = true
        sliderWordCount.addOnChangeListener { _, value, _ ->
            try {
                listenEditText = false
                if (listenSlider) {
                    wordCountText.setText(value.toInt().toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to set the word count value", e)
            } finally {
                listenEditText = true
            }
        }
        sliderWordCount.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            // TODO upgrade material-components lib
            // https://stackoverflow.com/questions/70873160/material-slider-onslidertouchlisteners-methods-can-only-be-called-from-within-t
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {}

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                generatePassphrase()
            }
        })
        wordCountText.doOnTextChanged { _, _, _, _ ->
            if (listenEditText) {
                try {
                    listenSlider = false
                    setSliderValue(getWordCount())
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to get the word count value", e)
                } finally {
                    listenSlider = true
                    generatePassphrase()
                }
            }
        }
        wordSeparator.doOnTextChanged { _, _, _, _ ->
            generatePassphrase()
        }
        wordCaseSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                generatePassphrase()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        generatePassphrase()

        mKeyGeneratorViewModel.passphraseGeneratedValidated.observe(viewLifecycleOwner) {
            mKeyGeneratorViewModel.setKeyGenerated(passKeyView.passwordString)
        }

        mKeyGeneratorViewModel.requirePassphraseGeneration.observe(viewLifecycleOwner) {
            generatePassphrase()
        }

        resetAppTimeoutWhenViewFocusedOrChanged(view)
    }

    private fun getWordCount(): Int {
        return try {
            Integer.valueOf(wordCountText.text.toString())
        } catch (numberException: NumberFormatException) {
            minSliderWordCount
        }
    }

    private fun setWordCount(wordCount: Int) {
        setSliderValue(wordCount)
        wordCountText.setText(wordCount.toString())
    }

    private fun setSliderValue(value: Int) {
        when {
            value < minSliderWordCount -> {
                sliderWordCount.value = minSliderWordCount.toFloat()
            }
            value > maxSliderWordCount -> {
                sliderWordCount.value = maxSliderWordCount.toFloat()
            }
            else -> {
                sliderWordCount.value = value.toFloat()
            }
        }
    }

    private fun getWordSeparator(): String {
        return wordSeparator.text.toString().ifEmpty { " " }
    }

    private fun getWordCase(): PassphraseGenerator.WordCase {
        var wordCase = PassphraseGenerator.WordCase.LOWER_CASE
        try {
            wordCase = PassphraseGenerator.WordCase.getByOrdinal(wordCaseSpinner.selectedItemPosition)
        } catch (caseException: Exception) {
            Log.e(TAG, "Unable to retrieve the word case", caseException)
        }
        return wordCase
    }

    private fun setWordCase(wordCase: PassphraseGenerator.WordCase) {
        wordCaseSpinner.setSelection(wordCase.ordinal)
    }

    private fun getSeparator(): String {
        return wordSeparator.text?.toString() ?: ""
    }

    private fun setSeparator(separator: String) {
        wordSeparator.setText(separator)
    }

    private fun generatePassphrase() {
        var passphrase = ""
        try {
            passphrase = PassphraseGenerator().generatePassphrase(
                getWordCount(),
                getWordSeparator(),
                getWordCase())
        } catch (e: Exception) {
            Log.e(TAG, "Unable to generate a passphrase", e)
        }
        passKeyView.passwordString = passphrase
        charactersCountText.text = getString(R.string.character_count, passphrase.length)
    }

    override fun onDestroy() {
        saveSettings()
        super.onDestroy()
    }

    private fun saveSettings() {
        context?.let { context ->
            PreferencesUtil.setDefaultPassphraseWordCount(context, getWordCount())
            PreferencesUtil.setDefaultPassphraseWordCase(context, getWordCase())
            PreferencesUtil.setDefaultPassphraseSeparator(context, getSeparator())
        }
    }

    private fun loadSettings() {
        context?.let { context ->
            setWordCount(PreferencesUtil.getDefaultPassphraseWordCount(context))
            setWordCase(PreferencesUtil.getDefaultPassphraseWordCase(context))
            setSeparator(PreferencesUtil.getDefaultPassphraseSeparator(context))
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        // Nothing here
    }

    companion object {
        private const val TAG = "PassphraseGnrtrFrgmt"
    }
}
