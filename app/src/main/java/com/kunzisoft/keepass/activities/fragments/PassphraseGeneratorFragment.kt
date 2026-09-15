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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.password.PassphraseGenerator
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.timeoutCopyToClipboard
import com.kunzisoft.keepass.utils.clear
import com.kunzisoft.keepass.view.PassphraseConditionsView
import com.kunzisoft.keepass.view.PasswordConditionsView
import com.kunzisoft.keepass.view.PasswordEditView
import com.kunzisoft.keepass.viewmodels.KeyGeneratorViewModel
import kotlinx.coroutines.launch

class PassphraseGeneratorFragment : DatabaseFragment() {

    private lateinit var passwordEditView: PasswordEditView
    private lateinit var passphraseConditionsView: PassphraseConditionsView
    private lateinit var passwordConditionsView: PasswordConditionsView

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
        passphraseConditionsView = view.findViewById(R.id.passphrase_words_conditions_view)
        passwordConditionsView = view.findViewById(R.id.passphrase_separator_conditions_view)

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
        }

        loadSettings()

        passphraseConditionsView.onConditionsChanged = {
            generatePassphrase()
        }
        passwordConditionsView.onConditionsChanged = {
            generatePassphrase()
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

    private fun generatePassphrase() {
        try {
            val passphrase = PassphraseGenerator().generatePassphrase(
                passphraseConditionsView.getWordCount(),
                getWordSeparator(),
                passphraseConditionsView.getWordCase()
            )
            passwordEditView.passwordCharArray = passphrase
            passphraseConditionsView.setCharacterCountText(getString(R.string.character_count, passphrase.size))
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
            PreferencesUtil.setDefaultPassphraseWordCount(context, passphraseConditionsView.getWordCount())
            PreferencesUtil.setDefaultPassphraseWordCase(context, passphraseConditionsView.getWordCase())
            PreferencesUtil.setDefaultPassphraseSeparatorOptions(context, passwordConditionsView.getOptions())
            PreferencesUtil.setDefaultPassphraseSeparatorLength(context, passwordConditionsView.getPasswordLength())
            PreferencesUtil.setDefaultPassphraseSeparatorConsiderChars(context, passwordConditionsView.getConsiderChars())
            PreferencesUtil.setDefaultPassphraseSeparatorIgnoreChars(context, passwordConditionsView.getIgnoreChars())
        }
    }

    private fun loadSettings() {
        context?.let { context ->
            passphraseConditionsView.setWordCount(PreferencesUtil.getDefaultPassphraseWordCount(context))
            passphraseConditionsView.setWordCase(PreferencesUtil.getDefaultPassphraseWordCase(context))
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
