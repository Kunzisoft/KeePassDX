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
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.timeoutCopyToClipboard
import com.kunzisoft.keepass.utils.clear
import com.kunzisoft.keepass.view.PasswordConditionsView
import com.kunzisoft.keepass.view.PasswordEditView
import com.kunzisoft.keepass.viewmodels.KeyGeneratorViewModel
import kotlinx.coroutines.launch

class PasswordGeneratorFragment : DatabaseFragment() {

    private lateinit var passwordEditView: PasswordEditView
    private lateinit var passwordConditionsView: PasswordConditionsView

    private val mKeyGeneratorViewModel: KeyGeneratorViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_generate_password, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        passwordEditView = view.findViewById(R.id.password_view)
        val passwordCopyView: ImageView? = view.findViewById(R.id.password_copy_button)

        passwordConditionsView = view.findViewById(R.id.password_conditions_view)
        passwordConditionsView.onConditionsChanged = {
            generatePassword()
        }

        context?.let { context ->
            passwordCopyView?.visibility = if (PreferencesUtil.allowCopyProtectedFields(context))
                View.VISIBLE else View.GONE
            passwordCopyView?.setOnClickListener {
                context.timeoutCopyToClipboard(
                    label = getString(R.string.password),
                    value = passwordEditView.passwordCharArray,
                    sensitive = true
                )
            }
        }

        loadSettings()

        // Pre-populate a password to possibly save the user a few clicks
        generatePassword()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mKeyGeneratorViewModel.passwordGeneratedValidated.collect {
                        mKeyGeneratorViewModel.setKeyGenerated(passwordEditView.passwordCharArray)
                    }
                }
                launch {
                    mKeyGeneratorViewModel.requirePasswordGeneration.collect {
                        generatePassword()
                    }
                }
            }
        }

        resetAppTimeoutWhenViewFocusedOrChanged(view)
    }

    private fun generatePassword() {
        try {
            val password = PasswordGenerator(resources).generatePassword(
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
            passwordEditView.passwordCharArray = password
            password.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to generate a password", e)
        }
    }

    override fun onDestroy() {
        saveSettings()
        super.onDestroy()
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        // Nothing here
    }

    private fun saveSettings() {
        context?.let { context ->
            PreferencesUtil.setDefaultPasswordOptions(context, passwordConditionsView.getOptions())
            PreferencesUtil.setDefaultPasswordLength(context, passwordConditionsView.getPasswordLength())
            PreferencesUtil.setDefaultPasswordConsiderChars(context, passwordConditionsView.getConsiderChars())
            PreferencesUtil.setDefaultPasswordIgnoreChars(context, passwordConditionsView.getIgnoreChars())
        }
    }

    private fun loadSettings() {
        context?.let { context ->
            passwordConditionsView.setOptions(PreferencesUtil.getDefaultPasswordOptions(context))
            passwordConditionsView.setPasswordLength(PreferencesUtil.getDefaultPasswordLength(context))
            passwordConditionsView.setConsiderChars(PreferencesUtil.getDefaultPasswordConsiderChars(context))
            passwordConditionsView.setIgnoreChars(PreferencesUtil.getDefaultPasswordIgnoreChars(context))
        }
    }

    companion object {
        private const val TAG = "PasswordGeneratorFrgmt"
    }
}
