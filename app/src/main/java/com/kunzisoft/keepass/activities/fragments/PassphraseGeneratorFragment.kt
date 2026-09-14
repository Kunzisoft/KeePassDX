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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.slider.Slider
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.password.PassphraseGenerator
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.timeoutCopyToClipboard
import com.kunzisoft.keepass.utils.clear
import com.kunzisoft.keepass.view.PasswordConditionsView
import com.kunzisoft.keepass.view.PasswordEditView
import com.kunzisoft.keepass.viewmodels.KeyGeneratorViewModel
import kotlinx.coroutines.launch

class PassphraseGeneratorFragment : DatabaseFragment() {

    private lateinit var passwordEditView: PasswordEditView
    private lateinit var passwordConditionsView: PasswordConditionsView

    private lateinit var sliderWordCount: Slider
    private lateinit var wordCountText: EditText
    private lateinit var charactersCountText: TextView
    private lateinit var wordCaseSpinner: Spinner

    private var minSliderWordCount: Int = 0
    private var maxSliderWordCount: Int = 0
    private var wordCaseAdapter: ArrayAdapter<String>? = null

    private val mKeyGeneratorViewModel: KeyGeneratorViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_generate_passphrase, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        passwordEditView = view.findViewById(R.id.passphrase_view)
        val passphraseCopyView: ImageView? = view.findViewById(R.id.passphrase_copy_button)
        sliderWordCount = view.findViewById(R.id.slider_word_count)
        wordCountText = view.findViewById(R.id.word_count)
        charactersCountText = view.findViewById(R.id.character_count)
        passwordConditionsView = view.findViewById(R.id.passphrase_separator_conditions_view)
        wordCaseSpinner = view.findViewById(R.id.word_case)

        minSliderWordCount = resources.getInteger(R.integer.passphrase_generator_word_count_min)
        maxSliderWordCount = resources.getInteger(R.integer.passphrase_generator_word_count_max)

        context?.let { context ->
            passphraseCopyView?.visibility = if (PreferencesUtil.allowCopyProtectedFields(context))
                View.VISIBLE else View.GONE
            passphraseCopyView?.setOnClickListener {
                context.timeoutCopyToClipboard(
                    label = getString(R.string.passphrase),
                    value = passwordEditView.passwordCharArray,
                    sensitive = true
                )
            }

            wordCaseAdapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                resources.getStringArray(R.array.word_case_array)
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            wordCaseSpinner.adapter = wordCaseAdapter
        }

        loadSettings()

        passwordConditionsView.onConditionsChanged = {
            generatePassphrase()
        }

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
        wordCaseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                generatePassphrase()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        generatePassphrase()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mKeyGeneratorViewModel.passphraseGeneratedValidated.collect {
                        mKeyGeneratorViewModel.setKeyGenerated(passwordEditView.passwordCharArray)
                    }
                }
                launch {
                    mKeyGeneratorViewModel.requirePassphraseGeneration.collect {
                        generatePassphrase()
                    }
                }
            }
        }

        resetAppTimeoutWhenViewFocusedOrChanged(view)
    }

    private fun getWordCount(): Int {
        return try {
            Integer.valueOf(wordCountText.text.toString())
        } catch (_: NumberFormatException) {
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
        return try {
            val generatedSeparator = PasswordGenerator(resources).generatePassword(
                length = passwordConditionsView.getPasswordLength(),
                upperCase = passwordConditionsView.isUppercaseChecked(),
                lowerCase = passwordConditionsView.isLowercaseChecked(),
                digits = passwordConditionsView.isDigitsChecked(),
                minus = passwordConditionsView.isMinusChecked(),
                underline = passwordConditionsView.isUnderlineChecked(),
                space = passwordConditionsView.isSpaceChecked(),
                specials = passwordConditionsView.isSpecialsChecked(),
                brackets = passwordConditionsView.isBracketsChecked(),
                extended = passwordConditionsView.isExtendedChecked(),
                considerChars = passwordConditionsView.getConsiderChars(),
                ignoreChars = passwordConditionsView.getIgnoreChars(),
                atLeastOneFromEach = passwordConditionsView.isAtLeastOneChecked(),
                excludeAmbiguousChar = passwordConditionsView.isExcludeAmbiguousChecked()
            )
            val separatorStr = String(generatedSeparator)
            generatedSeparator.clear()
            separatorStr.ifEmpty { " " }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to generate a separator", e)
            " "
        }
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

    private fun generatePassphrase() {
        try {
            val passphrase = PassphraseGenerator().generatePassphrase(
                getWordCount(),
                getWordSeparator(),
                getWordCase()
            )
            passwordEditView.passwordCharArray = passphrase
            charactersCountText.text = getString(R.string.character_count, passphrase.size)
            passphrase.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to generate a passphrase", e)
        }
    }

    override fun onDestroy() {
        saveSettings()
        super.onDestroy()
    }

    private fun saveSettings() {
        context?.let { context ->
            PreferencesUtil.setDefaultPassphraseWordCount(context, getWordCount())
            PreferencesUtil.setDefaultPassphraseWordCase(context, getWordCase())
            PreferencesUtil.setDefaultPassphraseSeparatorOptions(context, passwordConditionsView.getOptions())
            PreferencesUtil.setDefaultPassphraseSeparatorLength(context, passwordConditionsView.getPasswordLength())
            PreferencesUtil.setDefaultPassphraseSeparatorConsiderChars(context, passwordConditionsView.getConsiderChars())
            PreferencesUtil.setDefaultPassphraseSeparatorIgnoreChars(context, passwordConditionsView.getIgnoreChars())
        }
    }

    private fun loadSettings() {
        context?.let { context ->
            setWordCount(PreferencesUtil.getDefaultPassphraseWordCount(context))
            setWordCase(PreferencesUtil.getDefaultPassphraseWordCase(context))
            passwordConditionsView.setOptions(PreferencesUtil.getDefaultPassphraseSeparatorOptions(context))
            passwordConditionsView.setPasswordLength(PreferencesUtil.getDefaultPassphraseSeparatorLength(context))
            passwordConditionsView.setConsiderChars(PreferencesUtil.getDefaultPassphraseSeparatorConsiderChars(context))
            passwordConditionsView.setIgnoreChars(PreferencesUtil.getDefaultPassphraseSeparatorIgnoreChars(context))
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        // Nothing here
    }

    companion object {
        private val TAG = PassphraseGenerator::class.simpleName
    }
}
